package com.example.concurs_bumbaru_2023

import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.media.MediaPlayer
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.concurs_bumbaru_2023.ml.SsdMobilenetV11Metadata1
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException
import java.util.*


class Video : AppCompatActivity(), SurfaceTextureListener,
    OnVideoSizeChangedListener {

    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var tts: TextToSpeech
    var textureView: TextureView? = null
    private var mediaPlayer: MediaPlayer? = null
    var fileDescriptor: AssetFileDescriptor? = null
    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        tts = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                // Set language for TextToSpeech
                tts.language = Locale.US
                tts.setSpeechRate(1.0f)
                println("TTS Sucessfully initialized")
            }
        })
        imageView = findViewById(R.id.imageView)
        labels = FileUtil.loadLabels(this,"labels.txt")
        imageProcessor = ImageProcessor.Builder().add(
            ResizeOp(300,300,
                ResizeOp.ResizeMethod.BILINEAR)
        ).build()
        val model = SsdMobilenetV11Metadata1.newInstance(this)

        textureView = findViewById(R.id.textureView)
        textureView?.setSurfaceTextureListener(this)
        mediaPlayer = MediaPlayer()
        try {
            fileDescriptor = assets.openFd("video.mp4")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        loadModelInBackground()
    }

    private fun loadModelInBackground() {
        // Start a coroutine in the background
        GlobalScope.launch {
            // Measure time to load model
            val start = System.currentTimeMillis()
            model = SsdMobilenetV11Metadata1.newInstance(this@Video)
            val end = System.currentTimeMillis()
            println("Model loading time: ${end - start} ms")
        }
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        val surface = Surface(surfaceTexture)
        try {
            mediaPlayer!!.setSurface(surface)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaPlayer!!.setDataSource(fileDescriptor!!)
                mediaPlayer!!.prepareAsync()
                mediaPlayer!!.setOnPreparedListener { mediaPlayer!!.start() }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        bitmap = textureView?.bitmap!!

        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width
        paint.textSize = h/15f
        paint.strokeWidth = h/85f
        var x = 0
        scores.forEachIndexed { index, fl ->
            x = index
            x *= 4
            if(fl > 0.5){
                paint.setColor(colors.get(index))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                paint.style = Paint.Style.FILL

                val label = labels.get(classes.get(index).toInt())
                val score = fl.toString()
                val text = "$label"

                canvas.drawText(text, locations.get(x+1)*w, locations.get(x)*h, paint)
                if (tts.isSpeaking) {
                    tts.stop()
                }
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        imageView.setImageBitmap(mutable)
    }
    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {}
}