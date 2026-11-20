package com.example.musickeyboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private lateinit var tvStatus: TextView
    private val idSuara = mutableMapOf<String, Int>()
    private var jumlahTerisi = 0
    private val jumlahNada = NOTA_PUTIH.size + NOTA_HITAM.size

    // Warna tuts saat ditekan (biru), sesuai target tampilan.
    private val warnaTekan = Color.rgb(33, 150, 243)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val baris = findViewById<LinearLayout>(R.id.barisPutih)
        val piano = findViewById<FrameLayout>(R.id.piano)

        // 1. Siapkan SoundPool untuk bunyi pendek dengan latensi rendah.
        val atribut = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(atribut)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                jumlahTerisi++
                if (jumlahTerisi == jumlahNada) {
                    tvStatus.text = "Siap dimainkan"
                }
            }
        }

        // 2. Buat WAV dari kode, lalu muat semua nada (putih dan hitam).
        val folder = File(cacheDir, "nada").apply { mkdirs() }
        for (nota in NOTA_PUTIH + NOTA_HITAM) {
            val file = simpanWav(folder, nota)
            idSuara[nota.nama] = soundPool.load(file.absolutePath, 1)
        }

        // 3. Tuts putih: delapan tuts dengan lebar sama.
        NOTA_PUTIH.forEach { nota ->
            val tuts = buatTuts(nota, Color.WHITE, Color.BLACK)
            tuts.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            )
            baris.addView(tuts)
        }

        // 4. Tuts hitam: diletakkan di antara tuts putih setelah ukuran diketahui.
        piano.post {
            val lebarPutih = baris.width / NOTA_PUTIH.size.toFloat()
            val lebarHitam = lebarPutih * 0.6f
            val tinggiHitam = baris.height * 0.6f

            NOTA_HITAM.forEachIndexed { i, nota ->
                val tuts = buatTuts(nota, Color.BLACK, Color.WHITE)
                tuts.layoutParams = FrameLayout.LayoutParams(
                    lebarHitam.toInt(), tinggiHitam.toInt(), Gravity.TOP or Gravity.START
                )
                tuts.x = (POSISI_HITAM[i] + 1) * lebarPutih - lebarHitam / 2
                tuts.elevation = 8f // pastikan tuts hitam selalu di atas tuts putih
                piano.addView(tuts)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buatTuts(nota: Nota, warna: Int, warnaTeks: Int): Button {
        return Button(this).apply {
            text = nota.nama
            setBackgroundColor(warna)
            setTextColor(warnaTeks)
            stateListAnimator = null
            // Bunyi dimainkan saat jari menyentuh (bukan saat dilepas) agar responsif,
            // dan warna tuts berubah biru selama ditekan.
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        setBackgroundColor(warnaTekan)
                        mainkan(nota.nama)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        setBackgroundColor(warna)
                    }
                }
                true
            }
        }
    }

    private fun mainkan(nama: String) {
        val id = idSuara[nama] ?: return
        // volume kiri, volume kanan, prioritas, loop (0 = tidak diulang), kecepatan
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
        tvStatus.text = "Memainkan $nama"
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}
