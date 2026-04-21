package com.example.multithreadinglab

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Объявляем View элементы
    private lateinit var btnCalculate: Button
    private lateinit var btnLoadImages: Button
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var imagesContainer: LinearLayout

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем View через findViewById
        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnCalculate = findViewById(R.id.btnCalculate)
        btnLoadImages = findViewById(R.id.btnLoadImages)
        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)
        imagesContainer = findViewById(R.id.imagesContainer)
    }

    private fun setupListeners() {
        btnCalculate.setOnClickListener {
            performCalculations()
        }

        btnLoadImages.setOnClickListener {
            loadImagesFromNetwork()
        }
    }

    private fun generateRandomArray(size: Int): DoubleArray {
        val random = Random()
        return DoubleArray(size) {
            random.nextDouble() * 200 - 100
        }
    }

    private fun performCalculations() {
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        tvResult.text = "Выполняются вычисления..."

        executor.execute {
            try {
                val array = generateRandomArray(100)

                val sumNegatives = array.filter { it < 0 }.sum()

                var minIndex = 0
                var maxIndex = 0

                for (i in 1 until array.size) {
                    if (array[i] < array[minIndex]) minIndex = i
                    if (array[i] > array[maxIndex]) maxIndex = i
                }

                val start = if (minIndex < maxIndex) minIndex else maxIndex
                val end = if (minIndex < maxIndex) maxIndex else minIndex

                var productBetween = 1.0
                var hasElements = false

                for (i in start + 1 until end) {
                    productBetween *= array[i]
                    hasElements = true
                }

                if (!hasElements) {
                    productBetween = 0.0
                }

                val resultText = buildString {
                    append("Сумма отрицательных элементов: %.2f\n".format(sumNegatives))
                    append("Минимальный элемент: %.2f (индекс %d)\n".format(array[minIndex], minIndex))
                    append("Максимальный элемент: %.2f (индекс %d)\n".format(array[maxIndex], maxIndex))
                    append("Произведение между min и max: %.2f".format(productBetween))
                }

                mainHandler.post {
                    progressBar.visibility = View.GONE
                    tvResult.text = resultText
                    Toast.makeText(this@MainActivity, "Вычисления завершены!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при вычислениях", e)
                mainHandler.post {
                    progressBar.visibility = View.GONE
                    tvResult.text = "Ошибка при вычислениях"
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val imageUrls = listOf(
        "https://picsum.photos/400/300?random=1",
        "https://picsum.photos/400/300?random=2",
        "https://picsum.photos/400/300?random=3",
        "https://picsum.photos/400/300?random=4",
        "https://picsum.photos/400/300?random=5"
    )

    private fun loadImageFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            connection.disconnect()

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения: $urlString", e)
            null
        }
    }

    private fun loadImagesFromNetwork() {
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        imagesContainer.removeAllViews()

        executor.execute {
            val totalImages = imageUrls.size
            var loadedCount = 0

            imageUrls.forEachIndexed { index, url ->
                try {
                    val bitmap = loadImageFromUrl(url)

                    if (bitmap != null) {
                        loadedCount++
                        mainHandler.post {
                            addImageViewToContainer(bitmap, index + 1)
                        }
                    } else {
                        mainHandler.post {
                            Toast.makeText(
                                this@MainActivity,
                                "Не удалось загрузить изображение ${index + 1}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    val progress = ((index + 1) * 100) / totalImages
                    mainHandler.post {
                        progressBar.progress = progress
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке изображения ${index + 1}", e)
                    mainHandler.post {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка загрузки изображения ${index + 1}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            mainHandler.post {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "Загружено $loadedCount из $totalImages изображений",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addImageViewToContainer(bitmap: Bitmap, index: Int) {
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
        }

        imagesContainer.addView(imageView)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}