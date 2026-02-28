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
        // Ages 3-5: Very simple
        WordQ("Which word rhymes with 'Cat'?", "Hat", listOf("Dog", "Ball", "Car"), 3),
        WordQ("Which word rhymes with 'Dog'?", "Frog", listOf("Cat", "Bird", "Fish"), 3),
        WordQ("Which word rhymes with 'Sun'?", "Fun", listOf("Moon", "Star", "Sky"), 4),
        WordQ("Which word rhymes with 'Ball'?", "Tall", listOf("Small", "Big", "Run"), 4),
        WordQ("What sound does a cow make?", "Moo", listOf("Woof", "Meow", "Oink"), 3),
        WordQ("What sound does a dog make?", "Woof", listOf("Moo", "Meow", "Quack"), 3),
        
        // Ages 5-6: Simple spelling and opposites
        WordQ("Spell the fruit: A_ple", "Apple", listOf("Aple", "Appel", "Apel"), 5),
        WordQ("Spell the color: R_d", "Red", listOf("Rad", "Rid", "Rod"), 5),
        WordQ("Spell the animal: C_t", "Cat", listOf("Cot", "Cut", "Cit"), 5),
        WordQ("What is the opposite of 'Hot'?", "Cold", listOf("Warm", "Fire", "Ice"), 5),
        WordQ("What is the opposite of 'Big'?", "Small", listOf("Huge", "Giant", "Tall"), 5),
        WordQ("What is the opposite of 'Up'?", "Down", listOf("Over", "In", "Out"), 5),
        WordQ("What is the opposite of 'Happy'?", "Sad", listOf("Angry", "Joy", "Cry"), 5),
        WordQ("What is the opposite of 'Fast'?", "Slow", listOf("Quick", "Run", "Speed"), 5),
        
        // Ages 6-7: More spelling
        WordQ("Spell the animal: T_ger", "Tiger", listOf("Tager", "Teger", "Tugger"), 6),
        WordQ("Spell the animal: El_phant", "Elephant", listOf("Elefant", "Eliphant", "Elephent"), 6),
        WordQ("Spell the fruit: B_nana", "Banana", listOf("Banena", "Benana", "Bonana"), 6),
        WordQ("Spell the animal: M_nkey", "Monkey", listOf("Mankey", "Munkey", "Monkee"), 6),
        WordQ("What is the opposite of 'Day'?", "Night", listOf("Morning", "Evening", "Noon"), 6),
        WordQ("What is the opposite of 'Open'?", "Close", listOf("Shut", "Lock", "Door"), 6),
        WordQ("What is the opposite of 'Young'?", "Old", listOf("New", "Baby", "Kid"), 6),
        
        // Ages 7-8: Synonyms
        WordQ("What is a synonym for 'Start'?", "Begin", listOf("End", "Stop", "Finish"), 7),
        WordQ("What is a synonym for 'Fast'?", "Quick", listOf("Slow", "Late", "Lazy"), 7),
        WordQ("What is a synonym for 'Big'?", "Large", listOf("Small", "Tiny", "Short"), 7),
        WordQ("What is a synonym for 'Smart'?", "Clever", listOf("Dumb", "Slow", "Weak"), 7),
        WordQ("What is a synonym for 'Happy'?", "Joyful", listOf("Sad", "Angry", "Upset"), 7),
        WordQ("What is a synonym for 'Angry'?", "Mad", listOf("Happy", "Calm", "Quiet"), 7),
        
        // Ages 8-10: Harder vocabulary
        WordQ("What is a synonym for 'Beautiful'?", "Gorgeous", listOf("Ugly", "Plain", "Simple"), 8),
        WordQ("What is the opposite of 'Ancient'?", "Modern", listOf("Old", "Historic", "Classic"), 8),
        WordQ("What is a synonym for 'Brave'?", "Courageous", listOf("Scared", "Timid", "Shy"), 8),
        WordQ("Spell correctly:", "Necessary", listOf("Neccessary", "Necesary", "Neccesary"), 9),
        WordQ("Spell correctly:", "Beautiful", listOf("Beautifull", "Beutiful", "Beautful"), 9),
        WordQ("What is a synonym for 'Enormous'?", "Huge", listOf("Tiny", "Small", "Mini"), 9),
        WordQ("What is the opposite of 'Generous'?", "Selfish", listOf("Kind", "Giving", "Nice"), 10),
        WordQ("What is a synonym for 'Difficult'?", "Challenging", listOf("Easy", "Simple", "Basic"), 10)
    )
    
    private fun generateVocabularyChallenge(age: Int): Challenge {
        val suitable = vocabList.filter { it.minAge <= age }
        val item = if(suitable.isNotEmpty()) suitable.random() else vocabList.minByOrNull { it.minAge }!!
        return createTextChallenge(item)
    }

    // --- GENERAL KNOWLEDGE ---
    private val gkList = listOf(
        // Ages 3-5: Very basic
        WordQ("What color is the sky?", "Blue", listOf("Red", "Green", "Yellow"), 3),
        WordQ("What color is grass?", "Green", listOf("Blue", "Red", "Yellow"), 3),
        WordQ("How many fingers do you have?", "10", listOf("5", "8", "12"), 3),
        WordQ("What animal says 'Meow'?", "Cat", listOf("Dog", "Cow", "Bird"), 3),
        WordQ("What animal says 'Woof'?", "Dog", listOf("Cat", "Pig", "Duck"), 3),
        WordQ("What do you drink from a cup?", "Water", listOf("Food", "Air", "Light"), 4),
        WordQ("What shape is a ball?", "Round", listOf("Square", "Triangle", "Flat"), 4),
        WordQ("What do we see at night?", "Stars", listOf("Sun", "Rainbow", "Clouds"), 4),
        
        // Ages 5-6: Nature & Animals
        WordQ("What color do you get mixing Blue and Yellow?", "Green", listOf("Red", "Purple", "Orange"), 5),
        WordQ("What color do you get mixing Red and Yellow?", "Orange", listOf("Green", "Purple", "Blue"), 5),
        WordQ("How many legs does a dog have?", "4", listOf("2", "6", "8"), 5),
        WordQ("What do bees make?", "Honey", listOf("Milk", "Silk", "Wool"), 5),
        WordQ("Where do fish live?", "Water", listOf("Trees", "Caves", "Sky"), 5),
        WordQ("What comes after Monday?", "Tuesday", listOf("Wednesday", "Sunday", "Friday"), 5),
        WordQ("How many legs does a spider have?", "8", listOf("6", "4", "10"), 6),
        WordQ("What is the color of a banana?", "Yellow", listOf("Red", "Blue", "Green"), 5),
        WordQ("Which season comes after winter?", "Spring", listOf("Summer", "Autumn", "Winter"), 6),
        WordQ("What do cows drink?", "Water", listOf("Milk", "Juice", "Tea"), 6),
        
        // Ages 7-8: Science & Geography
        WordQ("Which animal is the fastest?", "Cheetah", listOf("Lion", "Tiger", "Elephant"), 7),
        WordQ("Which is the biggest animal on land?", "Elephant", listOf("Giraffe", "Lion", "Bear"), 7),
        WordQ("What do plants need to grow?", "Sunlight", listOf("Darkness", "Ice", "Fire"), 7),
        WordQ("How many colors are in a rainbow?", "7", listOf("5", "6", "8"), 7),
        WordQ("What is frozen water called?", "Ice", listOf("Steam", "Cloud", "Rain"), 7),
        WordQ("Which planet do we live on?", "Earth", listOf("Mars", "Moon", "Sun"), 7),
        WordQ("How many days in a week?", "7", listOf("5", "6", "10"), 7),
        WordQ("What is the largest land animal?", "Elephant", listOf("Whale", "Giraffe", "Hippo"), 8),
        WordQ("How many days in a year?", "365", listOf("360", "100", "500"), 8),
        WordQ("Which season is coldest?", "Winter", listOf("Summer", "Spring", "Autumn"), 8),
        
        // Ages 9-10: World Facts & Science
        WordQ("Which planet is closest to the Sun?", "Mercury", listOf("Venus", "Earth", "Mars"), 9),
        WordQ("What is the largest planet?", "Jupiter", listOf("Saturn", "Neptune", "Earth"), 9),
        WordQ("Which gas do we breathe?", "Oxygen", listOf("Carbon", "Nitrogen", "Helium"), 9),
        WordQ("How many continents are there?", "7", listOf("5", "6", "8"), 9),
        WordQ("Which is the largest ocean?", "Pacific", listOf("Atlantic", "Indian", "Arctic"), 10),
        WordQ("What is the capital of France?", "Paris", listOf("London", "Rome", "Berlin"), 10),
        WordQ("What is the capital of India?", "New Delhi", listOf("Mumbai", "Kolkata", "Chennai"), 9),
        WordQ("What is the capital of USA?", "Washington DC", listOf("New York", "Los Angeles", "Chicago"), 10),
        WordQ("What is the largest country by area?", "Russia", listOf("China", "USA", "Canada"), 10),
        WordQ("How many bones in the human body?", "206", listOf("200", "150", "300"), 10),
        WordQ("Which is the longest river?", "Nile", listOf("Amazon", "Ganges", "Mississippi"), 10),
        WordQ("How many planets in our solar system?", "8", listOf("9", "7", "10"), 9),
        WordQ("What is the hardest natural substance?", "Diamond", listOf("Gold", "Iron", "Silver"), 10)
    )
    
    private fun generateGKChallenge(age: Int): Challenge {
        val suitable = gkList.filter { it.minAge <= age }
        val item = if(suitable.isNotEmpty()) suitable.random() else gkList.minByOrNull { it.minAge }!!
        return createTextChallenge(item)
    }

    // --- TRANSLATION ---
    private val transList = listOf(
        // Spanish - Basic
        WordQ("Translate 'DOG' to Spanish", "Perro", listOf("Gato", "Pajaro", "Pez"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'CAT' to Spanish", "Gato", listOf("Perro", "Raton", "Leon"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'HELLO' to Spanish", "Hola", listOf("Adios", "Gracias", "Si"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'GOODBYE' to Spanish", "Adios", listOf("Hola", "Gracias", "Por favor"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'WATER' to Spanish", "Agua", listOf("Leche", "Jugo", "Cafe"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'MILK' to Spanish", "Leche", listOf("Agua", "Jugo", "Pan"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'RED' to Spanish", "Rojo", listOf("Azul", "Verde", "Negro"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'BLUE' to Spanish", "Azul", listOf("Rojo", "Verde", "Amarillo"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'GREEN' to Spanish", "Verde", listOf("Rojo", "Azul", "Blanco"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'BANANA' to Spanish", "Platano", listOf("Manzana", "Naranja", "Uva"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'APPLE' to Spanish", "Manzana", listOf("Platano", "Naranja", "Pera"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'HOUSE' to Spanish", "Casa", listOf("Carro", "Arbol", "Libro"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate 'BOOK' to Spanish", "Libro", listOf("Casa", "Mesa", "Silla"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate 'THANK YOU' to Spanish", "Gracias", listOf("Hola", "Adios", "Por favor"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate 'PLEASE' to Spanish", "Por favor", listOf("Gracias", "Hola", "Adios"), 7, SupportedLanguage.SPANISH),
        
        // French - Basic
        WordQ("Translate 'DOG' to French", "Chien", listOf("Chat", "Oiseau", "Souris"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'CAT' to French", "Chat", listOf("Chien", "Lion", "Tigre"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'HELLO' to French", "Bonjour", listOf("Au revoir", "Merci", "Oui"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'GOODBYE' to French", "Au revoir", listOf("Bonjour", "Merci", "S'il vous plait"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'WATER' to French", "Eau", listOf("Lait", "Jus", "Pain"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'MILK' to French", "Lait", listOf("Eau", "Jus", "Cafe"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'RED' to French", "Rouge", listOf("Bleu", "Vert", "Noir"), 5, SupportedLanguage.FRENCH),
        WordQ("Translate 'BLUE' to French", "Bleu", listOf("Rouge", "Vert", "Jaune"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'GREEN' to French", "Vert", listOf("Rouge", "Bleu", "Blanc"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'APPLE' to French", "Pomme", listOf("Banane", "Orange", "Raisin"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'HOUSE' to French", "Maison", listOf("Voiture", "Arbre", "Livre"), 7, SupportedLanguage.FRENCH),
        WordQ("Translate 'BOOK' to French", "Livre", listOf("Maison", "Table", "Chaise"), 7, SupportedLanguage.FRENCH),
        WordQ("Translate 'THANK YOU' to French", "Merci", listOf("Bonjour", "Au revoir", "Oui"), 7, SupportedLanguage.FRENCH),
        WordQ("Translate 'YES' to French", "Oui", listOf("Non", "Merci", "Bonjour"), 6, SupportedLanguage.FRENCH),
        WordQ("Translate 'NO' to French", "Non", listOf("Oui", "Merci", "Au revoir"), 6, SupportedLanguage.FRENCH),

        // German - Basic
        WordQ("Translate 'DOG' to German", "Hund", listOf("Katze", "Vogel", "Maus"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'CAT' to German", "Katze", listOf("Hund", "Löwe", "Tiger"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'HELLO' to German", "Hallo", listOf("Tschüss", "Danke", "Ja"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'GOODBYE' to German", "Tschüss", listOf("Hallo", "Danke", "Bitte"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'WATER' to German", "Wasser", listOf("Milch", "Saft", "Brot"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'MILK' to German", "Milch", listOf("Wasser", "Saft", "Kaffee"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'RED' to German", "Rot", listOf("Blau", "Grün", "Schwarz"), 5, SupportedLanguage.GERMAN),
        WordQ("Translate 'BLUE' to German", "Blau", listOf("Rot", "Grün", "Gelb"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'GREEN' to German", "Grün", listOf("Rot", "Blau", "Weiß"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'APPLE' to German", "Apfel", listOf("Banane", "Orange", "Birne"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'HOUSE' to German", "Haus", listOf("Auto", "Baum", "Buch"), 7, SupportedLanguage.GERMAN),
        WordQ("Translate 'BOOK' to German", "Buch", listOf("Haus", "Tisch", "Stuhl"), 7, SupportedLanguage.GERMAN),
        WordQ("Translate 'THANK YOU' to German", "Danke", listOf("Hallo", "Tschüss", "Ja"), 7, SupportedLanguage.GERMAN),
        WordQ("Translate 'YES' to German", "Ja", listOf("Nein", "Danke", "Hallo"), 6, SupportedLanguage.GERMAN),
        WordQ("Translate 'NO' to German", "Nein", listOf("Ja", "Danke", "Tschüss"), 6, SupportedLanguage.GERMAN),
        
        // Hindi - Basic
        WordQ("Translate 'DOG' to Hindi", "कुत्ता", listOf("बिल्ली", "शेर", "हाथी"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'CAT' to Hindi", "बिल्ली", listOf("कुत्ता", "चूहा", "घोड़ा"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'HELLO' to Hindi", "नमस्ते", listOf("धन्यवाद", "हाँ", "नहीं"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'GOODBYE' to Hindi", "अलविदा", listOf("नमस्ते", "धन्यवाद", "हाँ"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'WATER' to Hindi", "पानी", listOf("दूध", "जूस", "चाय"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'MILK' to Hindi", "दूध", listOf("पानी", "जूस", "चाय"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'RED' to Hindi", "लाल", listOf("नीला", "हरा", "काला"), 5, SupportedLanguage.HINDI),
        WordQ("Translate 'BLUE' to Hindi", "नीला", listOf("लाल", "हरा", "पीला"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'GREEN' to Hindi", "हरा", listOf("लाल", "नीला", "सफेद"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'APPLE' to Hindi", "सेब", listOf("केला", "संतरा", "अंगूर"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'BANANA' to Hindi", "केला", listOf("सेब", "संतरा", "आम"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'HOUSE' to Hindi", "घर", listOf("गाड़ी", "पेड़", "किताब"), 7, SupportedLanguage.HINDI),
        WordQ("Translate 'BOOK' to Hindi", "किताब", listOf("घर", "मेज", "कुर्सी"), 7, SupportedLanguage.HINDI),
        WordQ("Translate 'THANK YOU' to Hindi", "धन्यवाद", listOf("नमस्ते", "अलविदा", "हाँ"), 7, SupportedLanguage.HINDI),
        WordQ("Translate 'YES' to Hindi", "हाँ", listOf("नहीं", "धन्यवाद", "नमस्ते"), 6, SupportedLanguage.HINDI),
        WordQ("Translate 'NO' to Hindi", "नहीं", listOf("हाँ", "धन्यवाद", "अलविदा"), 6, SupportedLanguage.HINDI)
    )

    private fun generateTranslationChallenge(age: Int): Challenge {
        // Filter by age AND selected languages
        val enabledLangs = SupportedLanguage.values().filter { settingsRepository.getLanguageEnabled(it) }
        
        // If nothing selected, default to Hindi
        val langsToUse = if(enabledLangs.isNotEmpty()) enabledLangs else listOf(SupportedLanguage.HINDI)
        
        val suitable = transList.filter { 
            it.minAge <= age && it.lang in langsToUse
        }
        
        val item = if(suitable.isNotEmpty()) {
            suitable.random() 
        } else {
             // Fallback: Try finding any enabled lang, ignoring age, or just any Hindi
             val anyLangSuitable = transList.filter { it.lang in langsToUse }
             if (anyLangSuitable.isNotEmpty()) {
                 anyLangSuitable.random()
             } else {
                 transList.filter { it.lang == SupportedLanguage.HINDI }.random()
             }
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
