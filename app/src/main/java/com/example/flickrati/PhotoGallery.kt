package com.example.flickrati

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.flikrati.databinding.ActivityPhotoGalleryBinding
import com.example.flickrati.viewModels.PhotoGalleryViewModel
import com.example.flikrati.R

class PhotoGallery : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityPhotoGalleryBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_photo_gallery
        )


        val isFragmentContainerEmpty = savedInstanceState == null
        if (isFragmentContainerEmpty) {
            supportFragmentManager
                .beginTransaction()
                .add(binding.fragmentContainer.id,
                    PhotoGalleryListFragment.newInstance()
                )
                .commit()
        }
    }
}
