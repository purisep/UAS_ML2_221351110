package com.example.strokeapps.ui.simulasi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.strokeapps.R
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SimulasiFragment : Fragment() {

    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var etAge: EditText
    private lateinit var cbHypertension: CheckBox
    private lateinit var cbHeartDisease: CheckBox
    private lateinit var rgMarried: RadioGroup
    private lateinit var rbMarried: RadioButton
    private lateinit var rbNotMarried: RadioButton
    private lateinit var spWorkType: Spinner
    private lateinit var rgResidence: RadioGroup
    private lateinit var rbUrban: RadioButton
    private lateinit var rbRural: RadioButton
    private lateinit var etGlucose: EditText
    private lateinit var etBmi: EditText
    private lateinit var spSmokingStatus: Spinner
    private lateinit var btnPredict: Button
    private lateinit var llResult: LinearLayout
    private lateinit var tvResult: TextView
    private lateinit var tvProbability: TextView

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    // Data arrays for spinners
    private val workTypes = arrayOf("Pilih Jenis Pekerjaan", "Private", "Self-employed", "Govt_job", "children", "Never_worked")
    private val smokingStatus = arrayOf("Pilih Status Merokok", "formerly smoked", "never smoked", "smokes", "Unknown")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeTensorFlowLite()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_simulasi, container, false)

        // Initialize UI components
        initializeViews(view)
        setupSpinners()
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        rgGender = view.findViewById(R.id.rgGender)
        rbMale = view.findViewById(R.id.rbMale)
        rbFemale = view.findViewById(R.id.rbFemale)
        etAge = view.findViewById(R.id.etAge)
        cbHypertension = view.findViewById(R.id.cbHypertension)
        cbHeartDisease = view.findViewById(R.id.cbHeartDisease)
        rgMarried = view.findViewById(R.id.rgMarried)
        rbMarried = view.findViewById(R.id.rbMarried)
        rbNotMarried = view.findViewById(R.id.rbNotMarried)
        spWorkType = view.findViewById(R.id.spWorkType)
        rgResidence = view.findViewById(R.id.rgResidence)
        rbUrban = view.findViewById(R.id.rbUrban)
        rbRural = view.findViewById(R.id.rbRural)
        etGlucose = view.findViewById(R.id.etGlucose)
        etBmi = view.findViewById(R.id.etBmi)
        spSmokingStatus = view.findViewById(R.id.spSmokingStatus)
        btnPredict = view.findViewById(R.id.btnPredict)
        llResult = view.findViewById(R.id.llResult)
        tvResult = view.findViewById(R.id.tvResult)
        tvProbability = view.findViewById(R.id.tvProbability)
    }

    private fun setupSpinners() {
        // Setup Work Type Spinner
        val workTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workTypes)
        workTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spWorkType.adapter = workTypeAdapter

        // Setup Smoking Status Spinner
        val smokingAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, smokingStatus)
        smokingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSmokingStatus.adapter = smokingAdapter
    }

    private fun setupClickListeners() {
        btnPredict.setOnClickListener {
            if (!isModelLoaded) {
                Toast.makeText(requireContext(), "Model belum dimuat. Silakan tunggu atau restart aplikasi.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (validateInputs()) {
                runPrediction()
            }
        }
    }

    private fun initializeTensorFlowLite() {
        try {
            val modelFile = loadModelFile()
            interpreter = Interpreter(modelFile)
            isModelLoaded = true
            Toast.makeText(requireContext(), "Model berhasil dimuat", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isModelLoaded = false
            interpreter = null
            Toast.makeText(requireContext(), "Error memuat model: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd("stroke_model_compatible.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun validateInputs(): Boolean {
        // Validate Gender
        if (rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate Age
        if (etAge.text.toString().isEmpty()) {
            etAge.error = "Umur harus diisi"
            return false
        }
        val age = etAge.text.toString().toIntOrNull()
        if (age == null || age < 0 || age > 120) {
            etAge.error = "Umur tidak valid (0-120)"
            return false
        }

        // Validate Marriage Status
        if (rgMarried.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Pilih status pernikahan", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate Work Type
        if (spWorkType.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Pilih jenis pekerjaan", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate Residence Type
        if (rgResidence.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Pilih tipe tempat tinggal", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate Glucose Level
        if (etGlucose.text.toString().isEmpty()) {
            etGlucose.error = "Kadar glukosa harus diisi"
            return false
        }
        val glucose = etGlucose.text.toString().toFloatOrNull()
        if (glucose == null || glucose < 0 || glucose > 500) {
            etGlucose.error = "Kadar glukosa tidak valid (0-500)"
            return false
        }

        // Validate BMI
        if (etBmi.text.toString().isEmpty()) {
            etBmi.error = "BMI harus diisi"
            return false
        }
        val bmi = etBmi.text.toString().toFloatOrNull()
        if (bmi == null || bmi < 10 || bmi > 100) {
            etBmi.error = "BMI tidak valid (10-100)"
            return false
        }

        // Validate Smoking Status
        if (spSmokingStatus.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Pilih status merokok", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun runPrediction() {
        try {
            // Double-check model is loaded before running prediction
            val currentInterpreter = interpreter
            if (currentInterpreter == null || !isModelLoaded) {
                Toast.makeText(requireContext(), "Model tidak tersedia untuk prediksi", Toast.LENGTH_LONG).show()
                return
            }

            // Prepare input data
            val inputData = prepareInputData()

            // Run inference
            val output = runInference(inputData, currentInterpreter)

            // Display results
            displayResults(output)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saat prediksi: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun prepareInputData(): FloatArray {
        val inputArray = FloatArray(10)

        // Gender: Male = 1, Female = 0
        inputArray[0] = if (rbMale.isChecked) 1.0f else 0.0f
        // Age
        inputArray[1] = etAge.text.toString().toFloat()
        // Hypertension: Yes = 1, No = 0
        inputArray[2] = if (cbHypertension.isChecked) 1.0f else 0.0f
        // Heart Disease: Yes = 1, No = 0
        inputArray[3] = if (cbHeartDisease.isChecked) 1.0f else 0.0f
        // Ever Married: Yes = 1, No = 0
        inputArray[4] = if (rbMarried.isChecked) 1.0f else 0.0f
        // Work Type (encoded as numbers)
        inputArray[5] = when (spWorkType.selectedItemPosition) {
            1 -> 2.0f // Private
            2 -> 3.0f // Self-employed
            3 -> 0.0f // Govt_job
            4 -> 4.0f // children
            5 -> 1.0f // Never_worked
            else -> 2.0f // Default to Private
        }
        // Residence Type: Urban = 1, Rural = 0
        inputArray[6] = if (rbUrban.isChecked) 1.0f else 0.0f
        // Average Glucose Level
        inputArray[7] = etGlucose.text.toString().toFloat()
        // BMI
        inputArray[8] = etBmi.text.toString().toFloat()
        // Smoking Status (encoded as numbers)
        inputArray[9] = when (spSmokingStatus.selectedItemPosition) {
            1 -> 1.0f // formerly smoked
            2 -> 2.0f // never smoked
            3 -> 3.0f // smokes
            4 -> 0.0f // Unknown
            else -> 2.0f // Default to never smoked
        }
        val mean = floatArrayOf(
            0.414383562f, 43.3532877f, 0.0971135029f, 0.0540606654f, 0.660469667f,
            2.17563601f, 0.50611546f, 106.317167f, 28.8879892f, 1.3683953f
        )
        val scale = floatArrayOf(
            0.49311161f, 22.59405159f, 0.29611226f, 0.22613737f, 0.47354988f,
            1.09010553f, 0.4999626f, 45.25411644f, 7.76252098f, 1.07192384f
        )
        for (i in inputArray.indices) {
            inputArray[i] = (inputArray[i] - mean[i]) / scale[i]
        }
        return inputArray
    }


    private fun runInference(inputData: FloatArray, interpreterInstance: Interpreter): FloatArray {
        // Prepare input and output tensors
        val input = Array(1) { inputData }
        val output = Array(1) { FloatArray(1) }
        // Run inference
        interpreterInstance.run(input, output)

        return output[0]
    }
    private fun displayResults(output: FloatArray) {
        val probability = output[0]
        val strokeRisk = probability > 0.3f

        // Show result layout
        llResult.visibility = View.VISIBLE

        // Display prediction result
        if (strokeRisk) {
            tvResult.text = "RISIKO TINGGI STROKE"
            tvResult.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            tvProbability.text = "Probabilitas: ${String.format("%.2f", probability * 100)}%"
        } else {
            tvResult.text = "RISIKO RENDAH STROKE"
            tvResult.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            tvProbability.text = "Probabilitas: ${String.format("%.2f", probability * 100)}%"
        }

        llResult.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}