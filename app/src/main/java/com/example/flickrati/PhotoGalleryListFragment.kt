package com.example.flickrati

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.flikrati.databinding.FragmentPhotoGalleryListBinding
import com.example.flikrati.databinding.ListItemPhotoGalleryListBinding
import com.example.flickrati.models.GalleryItem
import com.example.flickrati.utils.ImageDownloader
import com.example.flickrati.utils.PollWorker
import com.example.flickrati.viewModels.PhotoGalleryViewModel
import com.example.flikrati.R

private const val TAG = "Photo Gallery Fragment"

class PhotoGalleryListFragment : Fragment() {
    private lateinit var binding: FragmentPhotoGalleryListBinding
    private lateinit var imageDownloader: ImageDownloader<PhotoHolder>

    private val photoGalleryViewModel: PhotoGalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        val responseHandler = Handler()
        imageDownloader = ImageDownloader(responseHandler) { photoHolder, bitmap ->
            val drawable = BitmapDrawable(resources, bitmap)
            photoHolder.bindDrawable(drawable)
        }
        lifecycle.addObserver(imageDownloader.fragmentLifecycle)

//        val constraintViolationException = Constraints.Builder().setRequiredNetworkType()

        val workRequest = OneTimeWorkRequest.Builder(PollWorker::class.java).build()
        WorkManager.getInstance().enqueue(workRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(imageDownloader.fragmentLifecycle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_photo_gallery_list,
            container,
            false
        )

        binding.photoGalleryListRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer { galleryItems ->
                Log.d(TAG, "Have gallery items from ViewModel: $galleryItems")
                binding.photoGalleryListRecyclerView.adapter = PhotoAdapter(galleryItems)
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        Log.d(TAG, "Got a query: $query")
                        // Collapse Keyboard after searching
                        searchItem.collapseActionView()
                        searchView.onActionViewCollapsed()
                        // Fetch Images
                        photoGalleryViewModel.fetchPhotos(query.orEmpty())
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean = false
                }
            )
            setOnSearchClickListener {
                searchView.setQuery(photoGalleryViewModel.searchTerm, false)
            }
        }
    }

    private class PhotoHolder(binding: ListItemPhotoGalleryListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val bindDrawable: (Drawable) -> Unit = binding.galleryItemImage::setImageDrawable
    }

    private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {
        private lateinit var binding: ListItemPhotoGalleryListBinding

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            binding = DataBindingUtil.inflate(
                this@PhotoGalleryListFragment.layoutInflater,
                R.layout.list_item_photo_gallery_list,
                parent,
                false
            )
            return PhotoHolder(binding)
        }

        override fun getItemCount(): Int = galleryItems.size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val placeholder: Drawable =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_launcher_foreground
                ) ?: ColorDrawable()
            val galleryItem = galleryItems[position]
            imageDownloader.queueImage(holder, galleryItem.url)
            holder.bindDrawable(placeholder)
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = PhotoGalleryListFragment()
    }
}

