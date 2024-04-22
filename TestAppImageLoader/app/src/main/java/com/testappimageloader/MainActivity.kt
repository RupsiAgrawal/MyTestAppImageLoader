package com.testappimageloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = ImageAdapter()
        recyclerView.adapter = adapter

        // Fetch and load images
        FetchImagesTask().execute("https://acharyaprashant.org/api/v2/content/misc/media-coverages?limit=100")
    }

    inner class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

        private val images = ArrayList<Bitmap>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.imageView.setImageBitmap(images[position])
        }

        override fun getItemCount(): Int = images.size

        fun addImage(bitmap: Bitmap) {
            images.add(bitmap)
            notifyDataSetChanged()
        }

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }
    }

    inner class FetchImagesTask : AsyncTask<String, Void, List<Bitmap>>() {

        override fun doInBackground(vararg params: String?): List<Bitmap> {
            val apiUrl = params[0] ?: return emptyList()
            val images = mutableListOf<Bitmap>()

            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)

                val cacheDir = getDiskCacheDir(this@MainActivity, "images")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                for (i in 0 until jsonArray.length()) {
                    Log.d("lenght",jsonArray.length().toString())
                    val thumbnail = jsonArray.getJSONObject(i).getJSONObject("thumbnail")
                    val imageUrl = thumbnail.getString("domain") + "/" +
                            thumbnail.getString("basePath") + "/0/" + thumbnail.getString("key")
                    val imageFile = File(cacheDir, "image_$i.png")

                    if (!imageFile.exists()) {
                        // Download image and save to disk cache
                        val imageBitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())
                        imageBitmap?.let {
                            saveBitmapToFile(imageBitmap, imageFile)
                        }
                    }

                    val imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    images.add(imageBitmap)
                }

                inputStream.close()
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return images
        }

        override fun onPostExecute(result: List<Bitmap>?) {
            super.onPostExecute(result)
            if (result != null) {
                for (bitmap in result) {
                    adapter.addImage(bitmap)
                }
            }
        }

        private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error saving bitmap to file: ${e.message}")
            }
        }

        private fun getDiskCacheDir(context: Context, uniqueName: String): File {
            return File(context.cacheDir, uniqueName)
        }
    }
}