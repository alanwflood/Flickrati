package com.example.flickrati.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface FlickrApi {
    @GET("services/rest/?method=flickr.interestingness.getList")
    fun fetchPhotos(): Call<FetchPhotosResponse>

    @GET
    fun fetchUrlBytes(@Url url: String): Call<ResponseBody>

    @GET("services/rest/?method=flickr.photos.search&sort=relevance")
    fun searchPhotos(@Query("text") query: String): Call<FetchPhotosResponse>
}