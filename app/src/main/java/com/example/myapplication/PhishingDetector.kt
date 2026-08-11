package com.example.myapplication

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SYSTEM_PROMPT = """
당신은 스미싱(피싱 문자) 탐지 전문가입니다.
아래 규칙을 참고해서 판단하세요:
"계좌 정지", "본인인증", "당첨", "환급" 등 긴급성이나 이익을 유도하는 단어가 있으면 의심
출처가 불분명한 단축 URL이나 링크 클릭을 요구하면 의심
공식 기관(은행, 정부, 택배사 등)은 문자로 링크 클릭이나 개인정보 입력을 잘 요구하지 않음
단, 이런 단어가 있어도 문맥상 정상적인 요청(예약 확인, 지인 연락 등)이면 정상으로 판단
사용자가 문자 내용을 보내면, 반드시 아래 형식만 사용하고 다른 설명이나 안내는 절대 추가하지 마세요.

판정: [피싱 또는 정상 중 하나만]
근거: [한 문장으로 간단히]
"""

object PhishingDetector {
  private var engine: Engine? = null
  private var conversation: Conversation? = null

  val isReady: Boolean
    get() = conversation != null

  suspend fun initialize(context: Context) =
    withContext(Dispatchers.IO) {
      if (engine != null) return@withContext

      val modelPath = ModelDownloader.modelFile(context).absolutePath
      val newEngine =
        Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU(), maxNumTokens = 4096))
      newEngine.initialize()

      val newConversation =
        newEngine.createConversation(
          ConversationConfig(
            samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3),
            systemInstruction = Contents.of(listOf(Content.Text(SYSTEM_PROMPT))),
          )
        )

      engine = newEngine
      conversation = newConversation
    }

  /** Sends [smsText] to the model and returns its full response text. */
  suspend fun classify(smsText: String): String =
    withContext(Dispatchers.IO) {
      val activeConversation = conversation ?: return@withContext "모델이 초기화되지 않았습니다."
      val response = activeConversation.sendMessage(Contents.of(listOf(Content.Text(smsText))))
      response.toString()
    }

  fun cleanUp() {
    conversation?.close()
    engine?.close()
    conversation = null
    engine = null
  }
}
