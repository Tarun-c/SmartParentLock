package com.smartparentlock

import kotlin.random.Random

data class Challenge(
    val question: String,
    val options: List<String>, // For multiple choice
    val correctIndex: Int
)

class ChallengeManager(private val settingsRepository: SettingsRepository) {

    fun generateChallenge(type: ChallengeType, age: Int): Challenge {
        return when (type) {
            ChallengeType.MATH -> generateMathChallenge(age)
            ChallengeType.LOGIC -> generateLogicChallenge(age)
            ChallengeType.PATTERNS -> generatePatternChallenge(age)
            ChallengeType.VOCABULARY -> generateVocabularyChallenge(age)
            ChallengeType.GK -> generateGKChallenge(age)
            ChallengeType.TRANSLATION -> generateTranslationChallenge(age)
        }
    }

    // --- MATH ---
    private fun generateMathChallenge(age: Int): Challenge {
        // Adjust difficulty based on Age, but respect user selected operations
        
        // Operations available based on Settings (Default true for Add/Sub if nothing enabled)
        val ops = mutableListOf<Int>()
        if (settingsRepository.getMathSkill(MathOp.ADD)) ops.add(0)
        if (settingsRepository.getMathSkill(MathOp.SUB)) ops.add(1)
        if (settingsRepository.getMathSkill(MathOp.MUL)) ops.add(2)
        if (settingsRepository.getMathSkill(MathOp.DIV)) ops.add(3)

        // Fallback if user unselected everything
        if (ops.isEmpty()) ops.add(0)
        
        val operation = ops.random()
        var num1 = 0
        var num2 = 0
        var answer = 0
        var symbol = ""

        // Range based on Age
        val maxVal = when {
            age <= 5 -> 10
            age <= 7 -> 20
            age <= 9 -> 50
            else -> 100
        }

        when (operation) {
            0 -> { // Add
                num1 = (1..maxVal).random()
                num2 = (1..maxVal).random()
                answer = num1 + num2
                symbol = "+"
            }
            1 -> { // Subtract
                num1 = (1..maxVal).random()
                num2 = (1..maxVal).random()
                if (num1 < num2) { val t = num1; num1 = num2; num2 = t }
                answer = num1 - num2
                symbol = "-"
            }
            2 -> { // Multiply
                val limit = if (age <= 9) 5 else 12
                num1 = (2..limit).random()
                num2 = (2..10).random()
                answer = num1 * num2
                symbol = "×"
            }
            3 -> { // Divide
                num2 = (2..9).random() // divisor
                val limit = if (age <= 10) 5 else 12
                answer = (2..limit).random() // quotient
                num1 = num2 * answer
                symbol = "÷"
            }
        }

        val question = "$num1 $symbol $num2 = ?"
        val options = generateNumberDistractors(answer, age)

        return Challenge(question, options, options.indexOf(answer.toString()))
    }

    // --- PATTERNS ---
    private fun generatePatternChallenge(age: Int): Challenge {
        // Simpler patterns for young kids
        val type = if (age < 8) 0 else Random.nextInt(3)
        var question = ""
        var answer = 0
        
        when(type) {
             0 -> { // Arithmetic Progression (2, 4, 6, 8, ?)
                 val start = (1..10).random()
                 val diff = (2..5).random()
                 val seq = List(5) { start + it * diff }
                 question = "Sequence: ${seq[0]}, ${seq[1]}, ${seq[2]}, ${seq[3]}, ?"
                 answer = seq[4]
             }
             1 -> { // Geometric - only for older
                 val start = (1..3).random()
                 val multi = 2
                 val seq = List(5) { start * Math.pow(multi.toDouble(), it.toDouble()).toInt() }
                 question = "Sequence: ${seq[0]}, ${seq[1]}, ${seq[2]}, ${seq[3]}, ?"
                 answer = seq[4]
             }
             2 -> { // Fibonacci-ish
                 var a = (1..3).random()
                 var b = a
                 val seq = mutableListOf(a, b)
                 repeat(3) {
                     val next = a + b
                     seq.add(next)
                     a = b
                     b = next
                 }
                 question = "Sequence: ${seq[0]}, ${seq[1]}, ${seq[2]}, ${seq[3]}, ?"
                 answer = seq[4]
             }
        }
        
        val options = generateNumberDistractors(answer, age)
        return Challenge(question, options, options.indexOf(answer.toString()))
    }
    
    // --- LOGIC Maps to Pattern for now ---
    private fun generateLogicChallenge(age: Int): Challenge {
        return generatePatternChallenge(age)
    }

    // --- VOCABULARY ---
    private data class WordQ(val q: String, val a: String, val wrong: List<String>, val minAge: Int = 5, val lang: SupportedLanguage? = null)
    
    // ... vocabList and gkList can stay same, they just use default lang=null ...
    
    private val vocabList = listOf(
        WordQ("Spell the fruit: A_ple", "Apple", listOf("Aple", "Appel", "Apel"), 5),
        WordQ("Which word rhymes with 'Cat'?", "Hat", listOf("Dog", "Ball", "Car"), 5),
        WordQ("What is the opposite of 'Hot'?", "Cold", listOf("Warm", "Fire", "Ice"), 6),
        WordQ("What is the opposite of 'Big'?", "Small", listOf("Huge", "Giant", "Tall"), 6),
        WordQ("Spell the animal: T_ger", "Tiger", listOf("Tager", "Teger", "Tugger"), 7),
        WordQ("What is a synonym for 'Start'?", "Begin", listOf("End", "Stop", "Finish"), 8),
        WordQ("What is a synonym for 'Fast'?", "Quick", listOf("Slow", "Late", "Lazy"), 8),
        WordQ("What is the opposite of 'Happy'?", "Sad", listOf("Angry", "Joy", "Cry"), 8)
    )
    
    private fun generateVocabularyChallenge(age: Int): Challenge {
        val suitable = vocabList.filter { it.minAge <= age }
        val item = if(suitable.isNotEmpty()) suitable.random() else vocabList.minByOrNull { it.minAge }!!
        return createTextChallenge(item)
    }

    // --- GENERAL KNOWLEDGE ---
    private val gkList = listOf(
        WordQ("What color do you get mixing Blue and Yellow?", "Green", listOf("Red", "Purple", "Orange"), 5),
        WordQ("How many legs does a spider have?", "8", listOf("6", "4", "10"), 6),
        WordQ("What do bees make?", "Honey", listOf("Milk", "Silk", "Wool"), 6),
        WordQ("Which animal is the fastest?", "Cheetah", listOf("Lion", "Tiger", "Elephant"), 7),
        WordQ("How many days in a year?", "365", listOf("360", "100", "500"), 8),
        WordQ("Which planet is closest to the Sun?", "Mercury", listOf("Venus", "Earth", "Mars"), 9),
        WordQ("Which is the largest ocean?", "Pacific", listOf("Atlantic", "Indian", "Arctic"), 10),
        WordQ("What is the capital of France?", "Paris", listOf("London", "Rome", "Berlin"), 10)
    )
    
    private fun generateGKChallenge(age: Int): Challenge {
        val suitable = gkList.filter { it.minAge <= age }
        val item = if(suitable.isNotEmpty()) suitable.random() else gkList.minByOrNull { it.minAge }!!
        return createTextChallenge(item)
    }

    // --- TRANSLATION ---
    private val transList = listOf(
        // Spanish
        WordQ("Translate 'DOG' to Spanish", "Perro", listOf("Gato", "Pajaro", "Pez"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'CAT' to Spanish", "Gato", listOf("Perro", "Raton", "Leon"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'HELLO' to Spanish", "Hola", listOf("Adios", "Gracias", "Si"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate 'RED' to Spanish", "Rojo", listOf("Azul", "Verde", "Negro"), 6, SupportedLanguage.SPANISH),
        
        // French
        WordQ("Translate 'DOG' to French", "Chien", listOf("Chat", "Oiseau", "Souris"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'CAT' to French", "Chat", listOf("Chien", "Lion", "Tigre"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'HELLO' to French", "Bonjour", listOf("Au revoir", "Merci", "Oui"), 7, SupportedLanguage.FRENCH),
        WordQ("Translate 'RED' to French", "Rouge", listOf("Bleu", "Vert", "Noir"), 6, SupportedLanguage.FRENCH),

        // German
        WordQ("Translate 'DOG' to German", "Hund", listOf("Katze", "Vogel", "Maus"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'CAT' to German", "Katze", listOf("Hund", "Löwe", "Tiger"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'HELLO' to German", "Hallo", listOf("Tschüss", "Danke", "Ja"), 7, SupportedLanguage.GERMAN),
        WordQ("Translate 'RED' to German", "Rot", listOf("Blau", "Grün", "Schwarz"), 6, SupportedLanguage.GERMAN),
        
        // Hindi
        WordQ("Translate 'DOG' to Hindi", "कुत्ता", listOf("बिल्ली", "शेर", "हाथी"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'CAT' to Hindi", "बिल्ली", listOf("कुत्ता", "चूहा", "घोड़ा"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'HELLO' to Hindi", "नमस्ते", listOf("धन्यवाद", "हाँ", "नहीं"), 7, SupportedLanguage.HINDI),
        WordQ("Translate 'RED' to Hindi", "लाल", listOf("नीला", "हरा", "काला"), 6, SupportedLanguage.HINDI)
    )

    private fun generateTranslationChallenge(age: Int): Challenge {
        // Filter by age AND selected languages
        val enabledLangs = SupportedLanguage.values().filter { settingsRepository.getLanguageEnabled(it) }
        
        // If nothing selected, default to Spanish
        val langsToUse = if(enabledLangs.isNotEmpty()) enabledLangs else listOf(SupportedLanguage.SPANISH)
        
        val suitable = transList.filter { 
            it.minAge <= age && it.lang in langsToUse
        }
        
        val item = if(suitable.isNotEmpty()) suitable.random() else {
             // Fallback: Try finding any enabled lang, ignoring age, or just any Spanish
             transList.firstOrNull { it.lang in langsToUse } ?: transList.first { it.lang == SupportedLanguage.SPANISH }
        }
        return createTextChallenge(item)
    }

    // --- HELPERS ---

    private fun createTextChallenge(item: WordQ): Challenge {
        val options = (item.wrong.take(3) + item.a).shuffled()
        return Challenge(item.q, options, options.indexOf(item.a))
    }

    private fun generateNumberDistractors(correctAnswer: Int, age: Int): List<String> {
        val distractors = mutableSetOf<Int>()
        distractors.add(correctAnswer)
        
        while (distractors.size < 4) {
             val variance = if (age <= 7) 5 else if (age <= 10) 10 else 20
             val d = correctAnswer + Random.nextInt(-variance, variance)
             // Ensure distractor is non-negative if answer is non-negative (simple rule)
             if (d != correctAnswer && (correctAnswer < 0 || d >= 0)) {
                 distractors.add(d)
             }
        }
        
        return distractors.toList().shuffled().map { it.toString() }
    }
}
