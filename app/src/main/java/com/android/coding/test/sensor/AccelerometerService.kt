package com.android.coding.test.sensor


import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Environment
import android.os.IBinder
import android.util.JsonWriter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONException
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class AccelerometerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var toContinueSavingFiles: Boolean = true
    //Array#1 to collect sensor data
    private var postsOne = ArrayList<Post>()
    //Array#2 to collect sensor data
    private var postsTwo = ArrayList<Post>()
    //switcher to choose array#1 or array#2
    private var postOneOrTwo: Boolean = true
    //Array to collect all sensor data from all files
    private var postsForClipping = ArrayList<Post>()
    //current file list of all already created JSON files
    private var currentFileList = ArrayList<String>()
    // Start time clipping file
    private var startTimeClipping: Long = 0
    // Finish time clipping file
    private var finishTimeClipping: Long = 0
    // Time when app started to make sure that we use all actual JSON file for clipping
    private var startAppTime: Long = 0
    //
    private var afterSecondRecord: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // create sensor manager
        sensorManager = getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        // register sensor manager in faster way
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        GlobalScope.async {
            // start collecting sensor data in "3 minutes" base
            sensorDataRecording()
            // start clipping sensor data from created files every 24 hours
            sensorFinalDataDaily()
        }

        startAppTime = getCurrentTimeStampInMills()
    }


    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        toContinueSavingFiles = false
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (postOneOrTwo) {
            postsOne.add(
                Post(
                    System.currentTimeMillis(),
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            )
        } else {
            postsTwo.add(
                Post(
                    System.currentTimeMillis(),
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private suspend fun sensorDataRecording() {
        val startTime = System.currentTimeMillis()
        // suspend till first 3 minutes gone
        delay(180000)
        while (true) {
            val finishTime = System.currentTimeMillis()
            if (postOneOrTwo) {

                postOneOrTwo = false
                //create JSON file in download/SensorData folder
                jsonWriterForRecorder(postsOne, startTime, finishTime,"SensorData")
                postsOne.clear()
            } else {
                postOneOrTwo = true
                //create JSON file in download/SensorData folder
                jsonWriterForRecorder(postsTwo, startTime, finishTime,"SensorData")
                postsTwo.clear()
                afterSecondRecord = true
            }
            delay(180000)
        }
    }
    private suspend fun sensorFinalDataDaily() {
        // suspend till first 24 hours gone
        delay(86400000)
        while (true) {
            //create JSON file in download/SensorData folder
            recordFinalData()
            delay(86400000)
        }
    }

    fun recordFinalData()  {
        if (afterSecondRecord) {
            currentFileList = chooseCurrentFileList()
            currentFileList.sortBy { it }
        }
        if (!currentFileList.isEmpty()) {
            fillPostsForClipping()
            //Creating a final JSON file with all collected data
            sensorDataClipping(startTimeClipping, finishTimeClipping)
        }
    }

    fun getCurrentTimeStamp(): String? {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",Locale.US)
        sdfDate.setTimeZone(TimeZone.getTimeZone("UTC"))
        val now = Date()
        return sdfDate.format(now)
    }

    private fun getCurrentTimeStampInMills(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        val time = cal.timeInMillis
        System.currentTimeMillis()
        return time
    }

    fun jsonWriterForRecorder(
        posts: List<Post>,
        startTime: Long?,
        finishTime: Long?,
        folder: String?
    ): Unit {
        val externalDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/" + folder)
        //        Create the storage directory if it does not exist
        if (externalDirectory != null) {
            if (! externalDirectory.exists()){
                if (! externalDirectory.mkdirs()){
                    Log.d("error", "failed to create directory");
                }
            }
        }
        val fileName: String = "Sensor_" + getCurrentTimeStamp() + ".sns"
        val file = File(externalDirectory.getAbsolutePath() + "/", fileName)
        val outStream = FileOutputStream(file)
        val writer = JsonWriter(OutputStreamWriter(outStream, "UTF-8"))
        writer.setIndent(" ")
        writer.beginObject()
        writer.name("sensorData")
        writeMessagesArray(writer, posts)
        writer.name("start").value(startTime)
        writer.name("end").value(finishTime)
        writer.endObject()
        writer.flush()
        writer.close()

    }

    fun writeMessagesArray(
        writer: JsonWriter,
        posts: List<Post>
    ) {
        writer.beginArray()
        for (post in posts) {
            writeMessage(writer, post)
        }
        writer.endArray()
    }

    fun writeMessage(writer: JsonWriter, post: Post) {
        writer.beginObject()
        writer.name("t_sec").value(post.getTime())
        writer.name("x_acc").value(post.getX())
        writer.name("y_acc").value(post.getY())
        writer.name("z_acc").value(post.getZ())
        writer.endObject()
    }

    fun sensorDataClipping(
        startTime: Long?,
        finishTime: Long?
    ): Unit {
        //create JSON file in download/SensorDataFinal folder
        jsonWriterForRecorder(postsForClipping, startTime, finishTime,"SensorDataFinal")
    }

    private fun fillPostsForClipping() {

        for (fileName in currentFileList) {
            val parser = JSONParser()
            try {
                val obj = parser.parse(FileReader(fileName))
                val jsonObject: JSONObject = obj as JSONObject
                if (startTimeClipping == 0L) {
                    startTimeClipping = jsonObject.get("start") as Long
                }
                finishTimeClipping = jsonObject.get("end") as Long


                val sensorDataArray = jsonObject.get("sensorData") as JSONArray
                for (i in 0 until sensorDataArray.size) {
                    val oneReading = sensorDataArray[i] as JSONObject
                    val x = oneReading.get("x_acc") as Double
                    val y: Double = oneReading.get("y_acc") as Double
                    val z: Double = oneReading.get("z_acc") as Double
                    val xF = x.toFloat()
                    val yF = y.toFloat()
                    val zF = z.toFloat()
                    postsForClipping.add(
                        Post(
                            oneReading.get("t_sec") as Long,
                            xF,
                            yF,
                            zF
                        )
                    )
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun chooseCurrentFileList(): ArrayList<String> {
        val fileNamePathList = ArrayList<String>()
        File((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/SensorData")).getAbsolutePath()).walk()
            .forEach {
                if (it.absolutePath.takeLast(4).equals(".sns")) {
                    val s = it.absolutePath.dropLast(4)

                    if (timeChecker(s.takeLast(23))) {
                        fileNamePathList.add(it.absolutePath)
                    }
                }
            }
        return fileNamePathList
    }

    private fun timeChecker(string: String): Boolean {
        val a: Long = stringDateTomilliseconds(string)
        val b: Long = startAppTime
        val result: Boolean = a > b
        return result

    }

    fun stringDateTomilliseconds(date: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",Locale.US)
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
        try {
            val mDate = sdf.parse(date)
            val timeInMilliseconds = mDate.time
            return timeInMilliseconds
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return 0
    }
}
