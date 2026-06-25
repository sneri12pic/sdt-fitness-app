package com.stepandemianenko.sdtfitness.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.stepandemianenko.sdtfitness.App

/**
 * Launched when an NFC sticker holding an NDEF URI record `sdtfitness://quest/<questId>` is tapped.
 * Performs the quest action and finishes immediately (NoDisplay — never shows UI). Works even when
 * the app is closed; the OS tag dispatch starts us.
 *
 * Write a sticker with any NFC writer (e.g. "NFC Tools" → URI record): `sdtfitness://quest/creatine`.
 */
class NfcQuestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent) {
        val questId = intent.data?.lastPathSegment?.trim()?.lowercase()
        val container = (application as App).container

        if (container.accountSessionManager.activeAccountId.value == null) {
            toast("Open the app and sign in first")
            return
        }

        when (questId) {
            "creatine" -> {
                container.homeRepository.addTodayCreatinePortion()
                toast("Creatine portion logged")
            }
            "water" -> {
                container.homeRepository.addTodayWaterPortion()
                toast("Water portion logged")
            }
            // ponytail: add cases here as you make more stickers.
            else -> toast("Unknown quest: $questId")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
