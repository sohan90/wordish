package com.conversant.app.wordish.data.network

import com.conversant.app.wordish.data.network.responses.WordsUpdateResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface WordDataService {
    @GET("words")
    fun fetchWordsData(@Query("revision") currentRevision: Int): Observable<WordsUpdateResponse>
}