package com.example.skindetectorapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.skindetectorapp.databinding.ActivityInputBinding

class InputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the predicted disease from MainActivity
        val predictedDisease = intent.getStringExtra("predicted_disease") ?: "No disease predicted. Please scan an image first."

        // Load string arrays from strings.xml
        val severity = resources.getStringArray(R.array.severity_levels)
        val age = resources.getStringArray(R.array.age_groups)
        val body = resources.getStringArray(R.array.body_areas)
        val sensitive = resources.getStringArray(R.array.sensitive_areas)

        // Set up adapters for each dropdown
        binding.severityInput.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, severity))
        binding.ageInput.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, age))
        binding.bodyInput.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, body))
        binding.sensitiveInput.setAdapter(ArrayAdapter(this, R.layout.dropdown_item, sensitive))

        // Initially hide the sensitive area dropdown
        binding.sensitiveInputLayout.visibility = android.view.View.GONE

        // Show sensitive area dropdown if "Other Sensitive Areas" selected
        binding.bodyInput.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = body[position]
            binding.sensitiveInputLayout.visibility = if (selectedItem == "Other Sensitive Areas") android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.homeBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.SubmitButton.setOnClickListener {
            val selectedSeverity = binding.severityInput.text.toString()
            val selectedAge = binding.ageInput.text.toString()
            val selectedBody = binding.bodyInput.text.toString()
            val selectedSensitive = if (binding.sensitiveInputLayout.visibility == android.view.View.VISIBLE) binding.sensitiveInput.text.toString() else ""

            val treatment = generateTreatment(predictedDisease, selectedSeverity, selectedAge, selectedBody, selectedSensitive)
            binding.treatmentResultText.text = "Predicted Disease: $predictedDisease\n\n$treatment"
        }
    }

    private fun generateTreatment(
        disease: String,
        severity: String,
        age: String,
        body: String,
        sensitive: String
    ): String {

        if (disease == "Normal") {
            return """
                No disease detected. Skin appears healthy.
                Home care: Maintain hygiene and moisturize daily with fragrance-free lotion.
                OTC: Gentle hypoallergenic moisturizers.
                ⚠️ Note: General advice. Consult a dermatologist if necessary.
            """.trimIndent()
        }

        val ageNote = if (age == "Child (0–12)") "For children, use pediatric-approved, fragrance-free products." else ""
        val severityNote = if (severity == "Severe") "Seek medical attention promptly; severe cases may need prescription treatment." else ""

        return when (disease) {

            "Eczema" -> {
                val areaAdvice = when {
                    body == "Face / Cheeks" -> "Use gentle, non-comedogenic moisturizer. Avoid makeup and strong soaps."
                    body == "Lips" -> "Apply petroleum jelly only; avoid flavored/colored lip balms."
                    body == "Eyelids" -> "Use gentle moisturizers. Steroids only if prescribed."
                    body == "Ear" -> "Keep dry, apply light emollient outside ear."
                    body == "Hand / Palms" -> "Moisturize frequently; avoid harsh detergents."
                    body == "Feet / Soles" -> "Keep dry, use fragrance-free moisturizer."
                    body == "Chest / Torso" -> "Apply moisturizer after bathing."
                    body == "Scalp" -> "Use gentle shampoos; avoid scratching."
                    body == "Body" -> "Apply moisturizer after bathing."
                    body == "Nails" -> "Keep nails short; avoid biting or scratching."
                    body == "Other Sensitive Areas" && sensitive.isNotEmpty() -> when (sensitive) {
                        "Nipples" -> "Apply lanolin cream; avoid friction and wear cotton clothing."
                        "Breasts" -> "Keep clean, apply gentle moisturizer if needed."
                        "Vulva" -> "Use fragrance-free products; avoid soaps and irritation."
                        "Penis" -> "Keep dry; mild emollients only. Avoid steroid creams without medical advice."
                        "Groin / Buttocks" -> "Keep dry; gentle moisturizer only. Avoid friction."
                        else -> "Apply gentle moisturizer and keep area dry."
                    }
                    else -> "Apply moisturizer after bathing."
                }

                """
                Home care: Petroleum jelly or ceramide-based moisturizer; lukewarm baths and pat dry. $areaAdvice
                OTC: Short-term hydrocortisone cream (0.5-1%) if safe for area, oral antihistamines for itch.
                Avoid: Hot showers, wool, scented products, known allergens.
                ⚠️ Note: General advice. Check with a dermatologist if symptoms persist or worsen. $ageNote $severityNote
            """.trimIndent()
            }

            "Psoriasis" -> {
                val areaAdvice = when {
                    body == "Face / Cheeks" || sensitive == "Eyelids" -> "Use gentle, fragrance-free moisturizer; steroids only if prescribed."
                    body == "Hand / Palms" -> "Thick moisturizer; gloves to retain moisture."
                    body == "Feet / Soles" -> "Salicylic acid on safe areas; avoid broken skin."
                    body == "Ear" -> "Avoid picking; gentle shampoo for plaques."
                    else -> "Apply moisturizer to reduce scaling."
                }

                """
                Home care: Warm or oatmeal baths; moisturize after bathing. Avoid strong treatments on sensitive areas.
                OTC: Salicylic acid (2-6%) for safe areas, coal tar shampoos for scalp/body.
                ⚠️ Note: General guidance only. See dermatologist if plaques are painful, widespread, or persistent. $ageNote $severityNote
            """.trimIndent()
            }

            "Tinea" -> {
                val areaAdvice = when {
                    body == "Feet / Soles" -> "Athlete's foot: Keep dry, wear socks, antifungal cream/powder."
                    body == "Hand / Palms" -> "Keep area clean and dry; antifungal cream if needed."
                    body == "Scalp" -> "Use antifungal shampoo; oral therapy may be required."
                    body == "Face / Cheeks" -> "Shave carefully if beard affected; apply topical antifungal cream."
                    body == "Chest / Torso" || body == "Body" -> "Apply antifungal cream; keep area dry."
                    body == "Other Sensitive Areas" && sensitive.isNotEmpty() -> when (sensitive) {
                        "Nipples" -> "Keep dry; gentle antifungal cream if needed."
                        "Breasts" -> "Keep area clean; mild antifungal cream if needed."
                        "Vulva" -> "Apply antifungal cream; keep area dry. Avoid steroid creams."
                        "Penis" -> "Apply antifungal cream; keep area dry. Avoid steroid creams."
                        "Groin / Buttocks" -> "Apply antifungal cream; keep area dry. Avoid friction."
                        else -> "Apply antifungal cream; keep area dry."
                    }
                    else -> "Keep affected area clean and dry."
                }

                """
                Home care: Keep area dry and clean; avoid sharing towels. $areaAdvice
                OTC: Topical antifungal creams like clotrimazole or terbinafine (1–2 weeks). Oral therapy may be needed for scalp.
                ⚠️ Note: General guidance only. See dermatologist if infection spreads, recurs, or does not improve. $ageNote $severityNote
            """.trimIndent()
            }

            "Skin Cancer (Suspicious lesion)" -> {
                val areaAdvice = if (body == "Ear") "Protect with hats and sunscreen." else "Cover lesions and monitor changes."

                """
                ⚠️ Warning: No home remedies for skin cancer.
                Protect area, avoid sun, schedule urgent dermatologist evaluation. $areaAdvice
                OTC: Broad-spectrum sunscreen (SPF 30+) daily.
                Avoid: Tanning beds, excessive sun, ignoring changes in moles/lesions.
                When to see a doctor: Immediately for suspicious lesions. $ageNote $severityNote
            """.trimIndent()
            }

            else -> """
                General advice: Keep area clean and monitor for changes.
                OTC: Moisturizing lotion or gentle soap.
                ⚠️ Note: Check with dermatologist for any unusual skin changes. $ageNote $severityNote
            """.trimIndent()
        }
    }
}
