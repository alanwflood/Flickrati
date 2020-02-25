package com.example.flickrati.viewModels

import android.app.Application
import androidx.lifecycle.*
import com.example.flickrati.api.FlickrFetch
import com.example.flickrati.models.GalleryItem
import com.example.flickrati.utils.QueryPreferences

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {
    val galleryItemLiveData: LiveData<List<GalleryItem>>
    private val flickrFetch = FlickrFetch()
    private val mutableSearchTerm = MutableLiveData<String>()

    val searchTerm: String
        get() = mutableSearchTerm.value  ?: ""

    init {
        mutableSearchTerm.value = QueryPreferences.getStoredQuery(app)

        galleryItemLiveData =
            Transformations.switchMap(mutableSearchTerm) { searchTerm ->
                if (searchTerm.isBlank()) {
                    flickrFetch.fetchPhotos()
                } else {
                    flickrFetch.fetchPhotos(searchTerm)
                }
            }
    }

    fun fetchPhotos(query: String = "") {
        QueryPreferences.setStoredQuery(app, query)
        mutableSearchTerm.value = query
    }
}