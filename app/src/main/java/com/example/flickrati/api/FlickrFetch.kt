package com.example.flickrati.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.flickrati.models.GalleryItem
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "FlickrFetch"
typealias FlickrData = LiveData<List<GalleryItem>>

class FlickrFetch {
    private var flickrApi: FlickrApi

    init {
        // Add additional data to urls
        val client = OkHttpClient
            .Builder()
            .addInterceptor(FlickrPhotoInterceptor())
            .build()

        val retrofit: Retrofit = Retrofit
            .Builder()
            .baseUrl("https://api.flickr.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        flickrApi = retrofit.create(FlickrApi::class.java)
    }


    // Fetch most interesting photos
    fun fetchPhotos(): FlickrData {
        val flickrRequest: Call<FetchPhotosResponse> = flickrApi.fetchPhotos()
        return convertPhotoRequestToLiveData(flickrRequest)
    }

    // Fetch photos by search query
    fun fetchPhotos(query: String): FlickrData {
        val request: Call<FetchPhotosResponse> = flickrApi.searchPhotos(query)
        return convertPhotoRequestToLiveData(request)
    }

    @WorkerThread
    fun fetchPhoto(url: String): Bitmap? {
        val response: Response<ResponseBody> =
            flickrApi.fetchUrlBytes(url).execute()
        val bitmap = response.body()?.byteStream()?.use(BitmapFactory::decodeStream)
        return bitmap
    }

    private fun convertPhotoRequestToLiveData(flickrRequest: Call<FetchPhotosResponse>): FlickrData {
        val responseLiveData: MutableLiveData<List<GalleryItem>> = MutableLiveData()
        flickrRequest.enqueue(object : Callback<FetchPhotosResponse> {
            override fun onFailure(call: Call<FetchPhotosResponse>, t: Throwable) {
                Log.e(TAG, "Failed to fetch images", t)
            }

            override fun onResponse(
                call: Call<FetchPhotosResponse>,
                response: Response<FetchPhotosResponse>
            ) {
                val flickerResponse: FetchPhotosResponse? = response.body()
                val photosReponse: PhotosReponse? = flickerResponse?.photos

                var galleryItems: List<GalleryItem> = photosReponse?.galleryItems ?: mutableListOf()
                galleryItems = galleryItems.filterNot {
                    it.url.isBlank()
                }

                Log.d(TAG, "Found ${galleryItems.size} items")

                responseLiveData.value = galleryItems
            }
        })

        return responseLiveData
    }
}

class FetchPhotosResponse() {
    lateinit var photos: PhotosReponse
}

class PhotosReponse() {
    @SerializedName("photo")
    lateinit var galleryItems: List<GalleryItem>
}