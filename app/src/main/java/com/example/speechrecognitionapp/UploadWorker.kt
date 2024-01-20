package com.example.speechrecognitionapp

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.ConcurrentHashMap

class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    companion object {
        private val wordCountMap = ConcurrentHashMap<String, Int>()

        fun updateWordCount(word: String) {
            val count = wordCountMap.getOrDefault(word, 0) + 1
            wordCountMap[word] = count
            Log.d("WordCount", "Beseda '$word' se je pojavila $count-krat.")
        }

        fun getWordCounts(): Map<String, Int> {
            return wordCountMap.toMap()
        }

        fun getSortedWordCounts(): List<Pair<String, Int>> {
            return wordCountMap.entries.sortedByDescending { it.value }.map { it.toPair() }
        }

        fun uploadWordCountsToFirebase() {
            val firebaseDatabase = FirebaseDatabase.getInstance()
            val myRef = firebaseDatabase.getReference("WordCounts")
            // Uporaba funkcije getSortedWordCounts za urejanje besed
            myRef.setValue(getSortedWordCounts())
        }
    }

    override fun doWork(): Result {
        Log.d("UploadWorker", "Poslano na bazo")
        uploadWordCountsToFirebase()
        return Result.success()
    }
}
