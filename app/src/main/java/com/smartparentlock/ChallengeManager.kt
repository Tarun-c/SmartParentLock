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
            ChallengeType.TRICKY -> generateTrickyChallenge(age)
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
    
    // --- LOGIC ---
    private val logicList = listOf(
        // Ages 3-5: Categorizing & Basics
        WordQ("Which of these is an animal?", "Cow", listOf("Table", "Apple", "Car"), 3),
        WordQ("Which item is used to eat?", "Spoon", listOf("Shoe", "Book", "Hat"), 3),
        WordQ("Which is smaller?", "Mouse", listOf("Elephant", "Lion", "Bear"), 4),
        WordQ("What goes with a lock?", "Key", listOf("Wallet", "Phone", "Box"), 4),

        // Ages 5-6: Reasoning & Functionality
        WordQ("What do you use an umbrella for?", "Rain", listOf("Sun", "Wind", "Snow"), 5),
        WordQ("If a square has 4 sides, a triangle has...", "3 sides", listOf("4 sides", "5 sides", "6 sides"), 5),
        WordQ("What tells you the time?", "Clock", listOf("Book", "Pen", "Shoe"), 5),
        WordQ("Which is the odd one out?", "Car", listOf("Apple", "Banana", "Orange"), 6),

        // Ages 7-8: Analogies & Deductions
        WordQ("Bird is to Fly as Fish is to...", "Swim", listOf("Walk", "Run", "Crawl"), 7),
        WordQ("Happy is to Smile as Sad is to...", "Cry", listOf("Laugh", "Anger", "Yell"), 7),
        WordQ("Sun is to Day as Moon is to...", "Night", listOf("Sky", "Star", "Cloud"), 7),
        WordQ("If you have 2 apples, get 3 more, then eat 1. How many left?", "4", listOf("5", "3", "6"), 8),

        // Ages 9-10: Complex Logic
        WordQ("Book is to Reading as Fork is to...", "Eating", listOf("Cooking", "Drawing", "Sleeping"), 9),
        WordQ("If A > B, and B > C, who is the smallest?", "C", listOf("A", "B", "None"), 9),
        WordQ("What is the next number: 2, 4, 8, 16, ?", "32", listOf("24", "20", "18"), 9),
        WordQ("Puppy is to Dog as Kitten is to...", "Cat", listOf("Lion", "Tiger", "Mouse"), 9),
        WordQ("What is the next number: 10, 30, 50, 70, ?", "90", listOf("80", "100", "110"), 10),
        
        // --- Added from FirstCry IQ Test ---
        WordQ("What are clothes made of?", "Cloth", listOf("Wood", "Glass", "Paper"), 4),
        WordQ("What tastes most sour?", "Lemon", listOf("Apple", "Banana", "Orange"), 4),
        WordQ("Which of the following items is used while making concrete?", "Sand", listOf("Mud", "Plaster", "Asphalt"), 5),
        WordQ("If you are outside and your shadow is in front of you, the sun is?", "Behind you", listOf("In front of you", "Over your head", "On the side"), 8),
        
        // --- Added from Child IQ Test PPT ---
        WordQ("Which is used for making holes in wood?", "Drill", listOf("Pliers", "Hammer", "Plane"), 6),
        WordQ("Which is used when playing baseball?", "Mitt", listOf("Basket", "Racket", "Stick"), 5),
        WordQ("Which did people invent first?", "Boats", listOf("Cars", "Airplanes", "Trains"), 7),
        WordQ("Which cannot go around the earth?", "The Sun", listOf("The moon", "A boat", "A plane"), 8),
        WordQ("Which is largest?", "Cell", listOf("Electron", "Atom", "Molecule"), 9)
    )

    private fun generateLogicChallenge(age: Int): Challenge {
        // Find questions meant for this exact age, or slightly younger (up to 2 years)
        var suitable = logicList.filter { age - it.minAge in 0..2 }
        if (suitable.isEmpty()) {
            // Fallback to highest available if child is older than max questions
            suitable = logicList.filter { it.minAge <= age } 
        }
        val item = if(suitable.isNotEmpty()) suitable.random() else logicList.maxByOrNull { it.minAge }!!
        return createTextChallenge(item)
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
        var suitable = vocabList.filter { age - it.minAge in 0..2 }
        if (suitable.isEmpty()) {
            suitable = vocabList.filter { it.minAge <= age }
        }
        val item = if(suitable.isNotEmpty()) suitable.random() else vocabList.maxByOrNull { it.minAge }!!
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
        WordQ("What is the hardest natural substance?", "Diamond", listOf("Gold", "Iron", "Silver"), 10),
        
        // --- Added from FirstCry IQ Test ---
        WordQ("Which animal is a baby sheep?", "Lamb", listOf("Calf", "Kitten", "Puppy"), 4),
        WordQ("Which of these animals can jump the highest?", "Kangaroo", listOf("Donkey", "Lion", "Giraffe"), 4),
        WordQ("Who is the first Indian woman in space?", "Kalpana Chawla", listOf("Chitra Mandal", "Helen Keller", "Sarojini Naidu"), 5),
        WordQ("What are the colours of a rainbow?", "Violet, Indigo, Blue, Green, Yellow, Orange, Red", listOf("Blue, Green, Yellow, Red, Orange, White, Purple", "Blue, Pink, Orange, Yellow, Red, Magenta, Maroon", "Maroon, Purple, Violet, Pink, Yellow, Red, Orange"), 6),
        WordQ("Which is the highest mountain range in the world?", "Himalayas", listOf("Urals", "Rockies", "Andes"), 6),
        WordQ("Which is the longest snake in the world?", "Python", listOf("Garter", "Cobra", "Rattle snake"), 7),
        WordQ("What converts carbon dioxide into oxygen?", "Plant", listOf("Air", "Soil", "Light"), 7),
        WordQ("Where does wool come from?", "Sheep", listOf("Goat", "Lemur", "Rabbit"), 7),
        WordQ("Which of the following is a non-renewable resource?", "Coal", listOf("Sunlight", "Wind", "Water"), 8),
        
        // --- Added from SlideShare PPT ---
        WordQ("Which animal is known as the 'Ship of the Desert'?", "Camel", listOf("Horse", "Elephant", "Donkey"), 6),
        WordQ("What do you call a house made of ice?", "Igloo", listOf("Tent", "Cabin", "Hut"), 5),
        WordQ("Which country is called the land of the rising sun?", "Japan", listOf("India", "China", "Australia"), 8),
        WordQ("Which two parts of the body continue to grow for your entire life?", "Nose and Ears", listOf("Hands and Feet", "Eyes and Teeth", "Hair and Nails"), 9),
        WordQ("Who is the inventor of the Computer?", "Charles Babbage", listOf("Albert Einstein", "Isaac Newton", "Thomas Edison"), 9),
        WordQ("How many consonants are there in the English alphabet?", "21", listOf("26", "5", "20"), 7)
    )
    
    private fun generateGKChallenge(age: Int): Challenge {
        var suitable = gkList.filter { age - it.minAge in 0..2 }
        if (suitable.isEmpty()) {
            suitable = gkList.filter { it.minAge <= age }
        }
        val item = if(suitable.isNotEmpty()) suitable.random() else gkList.maxByOrNull { it.minAge }!!
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
        WordQ("Translate 'GOOD MORNING' to Spanish", "Buenos dias", listOf("Buenas noches", "Hola", "Adios"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'GOOD NIGHT' to Spanish", "Buenas noches", listOf("Buenos dias", "Hola", "Adios"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'YELLOW' to Spanish", "Amarillo", listOf("Verde", "Azul", "Rojo"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'WHITE' to Spanish", "Blanco", listOf("Negro", "Rojo", "Azul"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'BLACK' to Spanish", "Negro", listOf("Blanco", "Amarillo", "Verde"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'ONE' to Spanish", "Uno", listOf("Dos", "Tres", "Cuatro"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'TWO' to Spanish", "Dos", listOf("Uno", "Tres", "Cinco"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'THREE' to Spanish", "Tres", listOf("Uno", "Dos", "Cuatro"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'FOUR' to Spanish", "Cuatro", listOf("Dos", "Tres", "Cinco"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'FIVE' to Spanish", "Cinco", listOf("Uno", "Tres", "Cuatro"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'BIRD' to Spanish", "Pajaro", listOf("Pez", "Conejo", "Perro"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'FISH' to Spanish", "Pez", listOf("Pajaro", "Gato", "Perro"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'RABBIT' to Spanish", "Conejo", listOf("Gato", "Pez", "Pajaro"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'MUM' to Spanish", "Mama", listOf("Papa", "Hermana", "Abuela"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'DAD' to Spanish", "Papa", listOf("Mama", "Hermano", "Abuelo"), 5, SupportedLanguage.SPANISH),
        WordQ("Translate 'BROTHER' to Spanish", "Hermano", listOf("Hermana", "Papa", "Abuelo"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'SISTER' to Spanish", "Hermana", listOf("Hermano", "Mama", "Abuela"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'GRANDMOTHER' to Spanish", "Abuela", listOf("Abuelo", "Mama", "Hermana"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate 'GRANDFATHER' to Spanish", "Abuelo", listOf("Abuela", "Papa", "Hermano"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate \"HOW ARE YOU?\" to Spanish", "¿Como estas?", listOf("¿Como te llamas?", "Mucho gusto", "No entiendo"), 8, SupportedLanguage.SPANISH),
        WordQ("Translate \"MY NAME IS...\" to Spanish", "Me llamo...", listOf("Quiero...", "¿Como estas?", "De nada"), 8, SupportedLanguage.SPANISH),
        WordQ("Translate \"WHAT IS YOUR NAME?\" to Spanish", "¿Como te llamas?", listOf("¿Como estas?", "¿Que quieres comer?", "No entiendo"), 8, SupportedLanguage.SPANISH),
        WordQ("Translate \"NICE TO MEET YOU\" to Spanish", "Mucho gusto", listOf("De nada", "No entiendo", "Por favor"), 8, SupportedLanguage.SPANISH),
        WordQ("Translate \"I DO NOT UNDERSTAND\" to Spanish", "No entiendo", listOf("Mucho gusto", "De nada", "Quiero..."), 8, SupportedLanguage.SPANISH),
        WordQ("Translate 'BREAD' to Spanish", "El pan", listOf("La sopa", "El queso", "La leche"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'CHEESE' to Spanish", "El queso", listOf("El pan", "El zumo", "El helado"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'JUICE' to Spanish", "El zumo", listOf("El agua", "La leche", "La sopa"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'ICE CREAM' to Spanish", "El helado", listOf("El zumo", "El queso", "El pan"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate 'SOUP' to Spanish", "La sopa", listOf("El helado", "El agua", "El zumo"), 6, SupportedLanguage.SPANISH),
        WordQ("Translate \"WHAT DO YOU WANT TO EAT?\" to Spanish", "¿Que quieres comer?", listOf("¿Como te llamas?", "¿Como estas?", "La cuenta, por favor"), 9, SupportedLanguage.SPANISH),
        WordQ("Translate \"I WANT...\" to Spanish", "Quiero...", listOf("Me llamo...", "De nada", "No entiendo"), 8, SupportedLanguage.SPANISH),
        WordQ("Translate \"YOU ARE WELCOME\" to Spanish", "De nada", listOf("Gracias", "Por favor", "Mucho gusto"), 7, SupportedLanguage.SPANISH),
        WordQ("Translate \"THE BILL, PLEASE\" to Spanish", "La cuenta, por favor", listOf("¿Que quieres comer?", "No entiendo", "Mucho gusto"), 9, SupportedLanguage.SPANISH),
        
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
        
        var suitable = transList.filter { 
            age - it.minAge in 0..2 && it.lang in langsToUse
        }
        
        if (suitable.isEmpty()) {
            suitable = transList.filter { it.minAge <= age && it.lang in langsToUse }
        }
        
        val item = if(suitable.isNotEmpty()) {
            suitable.random() 
        } else {
             // Fallback: Try finding any enabled lang, ignoring age, or just any Hindi
             val anyLangSuitable = transList.filter { it.lang in langsToUse }
             if (anyLangSuitable.isNotEmpty()) {
                 anyLangSuitable.maxByOrNull { it.minAge } ?: anyLangSuitable.random()
             } else {
                 transList.filter { it.lang == SupportedLanguage.HINDI }.maxByOrNull { it.minAge } ?: transList.first()
             }
        }
        return createTextChallenge(item)
    }

    // --- TRICKY QUESTIONS ---
    private val trickyList = listOf(
        WordQ("A blue house has blue bricks; a yellow house has yellow bricks. What is a greenhouse made of?", "Glass", listOf("A coat of paint", "A starlight", "Tomorrow"), 7),
        WordQ("What type of dress can never be worn?", "Address", listOf("Concrete floors are hard to crack", "A hole", "A glacier"), 10),
        WordQ("What is a Tornado’s favorite game?", "Twister", listOf("A map", "Address", "A teabag"), 8),
        WordQ("What are the two things that we can’t eat before breakfast?", "Lunch and dinner", listOf("Wisdom", "A cold", "Noon"), 7),
        WordQ("If I have it, I don’t share it. If I share it, I don’t have it. What is it?", "A secret", listOf("Guilt", "A crane", "A river"), 6),
        WordQ("What word is spelled incorrectly in every single dictionary?", "Incorrectly", listOf("A keyboard", "A saddle", "Pilgrims"), 10),
        WordQ("I am full of holes, but I can hold water. Who am I?", "Sponge", listOf("Guilt", "A snowman", "Heat because you can always catch a cold."), 8),
        WordQ("What has many keys but can’t open a door?", "A piano", listOf("A pearl", "Deadend", "A garbage truck"), 9),
        WordQ("I love to dance and twist and prance, I shake my tail, as away I sail, wingless I fly into the sky. What am I?", "A kite", listOf("One, two, and three", "Letter V", "An umbrella"), 7),
        WordQ("What can’t be used until it is broken?", "A coconut", listOf("A book", "Flowers", "Wisdom"), 9),
        WordQ("What has one eye but cannot see?", "A needle", listOf("One, two, and three", "A friend", "Nine"), 6),
        WordQ("What is it the more you take, the larger it becomes?", "A hole", listOf("A coat of paint", "Second", "A stamp"), 9),
        WordQ("What gets wet while drying?", "Towel", listOf("A compass", "Wisdom", "A coffin"), 5),
        WordQ("What never asks a question but gets answered all the time?", "A telephone", listOf("A coconut", "Guilt", "A keyboard"), 5),
        WordQ("If a plane crashes on the border between the United States and Canada, where do they bury the survivors?", "Survivors are never buried", listOf("The President", "Glass", "Maggots"), 9),
        WordQ("How can you go 25 days without sleep?", "Sleep at night.", listOf("A tie", "A coffin", "A fire"), 8),
        WordQ("How do you make the number one disappear?", "Add a ‘G,’ and it’s gone!", listOf("The green elevator", "Rainbow", "A drum"), 10),
        WordQ("A cowboy rode into town on Friday. He stayed in town for three days and rode out on Friday. How is that possible?", "His horse is named Friday", listOf("A comb", "A rubber band", "Coconut trees don’t grow bananas."), 7),
        WordQ("If a green man lives in a greenhouse, a purple man lives in a purple house, a blue man lives in a blue house, a yellow man lives in a yellow house, a black man lives in a black house. Who lives in a White House?", "The President", listOf("Second", "Deadend", "The green elevator"), 6),
        WordQ("Why is the math book sad?", "Because it has problems", listOf("Outside", "Add a ‘G,’ and it’s gone!", "A mirror"), 6),
        WordQ("If a monkey, a squirrel, and a bird are racing to the top of a coconut tree, who will get the banana first?", "Coconut trees don’t grow bananas.", listOf("A mirror", "Sponge", "Blame"), 8),
        WordQ("What has four wheels and flies?", "A garbage truck", listOf("Playing chess with Kate.", "Dictionary", "Few"), 6),
        WordQ("What always goes to bed with its shoes on?", "A horse", listOf("Six", "A television", "A mirror"), 8),
        WordQ("If a rooster lays an egg on top of the barn roof, which way will it roll?", "Roosters do not lay eggs", listOf("Incorrectly", "A keyboard", "None. All birds fly away"), 10),
        WordQ("How can you lift an elephant with one hand?", "You cannot because the elephant does not have hands.", listOf("A peanut", "A rope", "The plate"), 7),
        WordQ("How can you drop a raw egg onto a concrete floor and not crack it?", "Concrete floors are hard to crack", listOf("Deadend", "Elephant’s shadow", "Lunch and dinner"), 7),
        WordQ("What moves faster: heat or cold?", "Heat because you can always catch a cold.", listOf("Noon", "A peanut", "An anchor"), 5),
        WordQ("Beth’s mother has three daughters. One is called Lara, and the other one is Sara. What is the name of the third daughter?", "Beth", listOf("Alligator", "A clock", "Mailbox"), 8),
        WordQ("What type of son does no parent want?", "An arson", listOf("Sponge", "A table", "Military time"), 9),
        WordQ("What kind of umbrella do most people carry on a rainy day?", "A wet one", listOf("Tomorrow", "Add a ‘G,’ and it’s gone!", "A fence"), 7),
        WordQ("What happens if you throw a white hat into the Black Sea?", "The hat gets wet", listOf("2 x 2 = 4, 2 + 2 = 4", "Second", "Seven"), 8),
        WordQ("He has married many women but has never been married. Who is he?", "A priest", listOf("Glass", "Lunch and dinner", "Letter “R”"), 10),
        WordQ("What tastes better than it smells?", "A tongue", listOf("Wholesome", "Seven", "December 31; today is January 1."), 7),
        WordQ("Shoot at me a thousand times, and I may still survive; one scratch from you and me will find your prospects take a dive. What am I?", "An eight ball", listOf("Incorrectly", "It sinks", "A clock"), 9),
        WordQ("What happens when you throw a blue rock into the yellow sea?", "It sinks", listOf("Stone", "A drum", "The four you took"), 9),
        WordQ("Which popular cheese is made backward?", "Edam", listOf("Parking lot", "The President", "Stone"), 10),
        WordQ("How can you physically stand behind your friend as he physically stands behind you?", "Back to back", listOf("A saddle", "A telephone", "Concrete floors are hard to crack"), 6),
        WordQ("Which bow can’t be tied?", "Rainbow", listOf("Glass", "Nothing. The wall is already built", "Outside"), 8),
        WordQ("What ship has no captain but two mates?", "Courtship", listOf("Your back", "Breath", "Light"), 9),
        WordQ("What is black when it’s clean and white when it’s dirty?", "A chalkboard", listOf("A weapon", "Corners", "Short"), 8),
        WordQ("What kind of street does a ghost like?", "Deadend", listOf("Heat because you can always catch a cold.", "Equal", "A chat room"), 7),
        WordQ("A deep well full of knives.", "Mouth", listOf("A horse", "Twister", "Letter “R”"), 5),
        WordQ("Although my cow is dead, I still beat her. What a racket she makes!", "A drum", listOf("Queue", "Glass", "Your name"), 9),
        WordQ("What’s the difference between a well-dressed man on a bicycle and a poorly-dressed man on a tricycle?", "A tire", listOf("Elephant’s shadow", "A tongue", "Nothing. The wall is already built"), 10),
        WordQ("If two’s company, and three’s a crowd, what are four and five?", "Nine", listOf("It sinks", "A map", "A secret"), 8),
        WordQ("Three doctors said that Bill was their brother. Bill says he has no brothers. How many brothers does Bill actually have?", "None. He has three sisters.", listOf("Equal", "The letter “E”", "Letter C"), 5),
        WordQ("Two fathers and two sons are in a car, yet there are only three people in the car. How?", "They are a grandfather, father, and son.", listOf("A peanut", "December 31; today is January 1.", "A cipher key"), 8),
        WordQ("What has hands but can’t clap?", "A clock", listOf("Dragon", "The hat gets wet", "A cipher key"), 7),
        WordQ("What has legs but doesn’t walk?", "A table", listOf("Towel", "A splinter", "Wholesome"), 9),
        WordQ("What has words but never speaks?", "A book", listOf("An anchor", "A pearl", "Letter V"), 5),
        WordQ("What has a thumb and four fingers but is not a hand?", "A glove", listOf("A tongue", "Beads", "Mouth"), 5),
        WordQ("What has a head and a tail but no body?", "A coin", listOf("A snowman", "Soul", "Mailbox"), 7),
        WordQ("What building has the most stories?", "The library", listOf("A crane", "Playing chess with Kate.", "Corn"), 8),
        WordQ("What has 13 hearts but no other organs?", "Pack of cards", listOf("Letter “E”", "A telephone", "A glacier"), 7),
        WordQ("What kind of coat is best put on wet?", "A coat of paint", listOf("A comb", "You have two apples", "Few"), 8),
        WordQ("I have no life, but I can die, what am I?", "A battery", listOf("Parking lot", "In watches", "Mouth"), 10),
        WordQ("What has lots of eyes but can’t see?", "A potato", listOf("A stamp", "Island", "Nine"), 9),
        WordQ("It stalks the countryside with ears that can’t hear. What is it?", "Corn", listOf("The hat gets wet", "The four you took", "Concrete floors are hard to crack"), 10),
        WordQ("What kind of band never plays music?", "A rubber band", listOf("Because it has problems", "A rope", "A chalkboard"), 7),
        WordQ("The captain took a bath without his belly getting wet.", "Canoe", listOf("A roof", "Wisdom", "The green elevator"), 7),
        WordQ("What kind of room doesn’t have physical walls?", "A chat room", listOf("Corners", "A glove", "An ant"), 7),
        WordQ("April showers bring May flowers. What do May flowers bring?", "Pilgrims", listOf("Isle", "A chain", "A television"), 6),
        WordQ("What is all over a house?", "A roof", listOf("Sleep at night.", "Seven", "A coin"), 10),
        WordQ("I saw a man in white; he looked quite a sight. He was not old, but he stood in the cold. And when he felt the sun, he started to run. Who could he be? Please answer me.", "A snowman", listOf("A map", "Nothing. The wall is already built", "Edam"), 7),
        WordQ("What does man love more than life, hate more than death or mortal strife that which contented men desire; the poor have, the rich require; the miser spends, the spendthrift saves, and all men carry to their graves?", "Nothing", listOf("Pilgrims", "Beads", "Peace"), 5),
        WordQ("If you sit a cup on the table facing south while you are on the north side of the table, on which side is the cup’s handle?", "Outside", listOf("Blame", "The library", "A syllable"), 8),
        WordQ("What two words, when combined, hold the most letters?", "Post office", listOf("An ant", "Beads", "Coconut trees don’t grow bananas."), 7),
        WordQ("What goes up as soon as the rain comes down?", "An umbrella", listOf("A coffin", "A ton", "Heat because you can always catch a cold."), 7),
        WordQ("What kind of tree can you carry in your hand?", "A palm tree", listOf("A potato", "Back to back", "A crane"), 6),
        WordQ("What’s as big as an elephant but weighs absolutely nothing?", "Elephant’s shadow", listOf("Silence", "2 x 2 = 4, 2 + 2 = 4", "The hat gets wet"), 9),
        WordQ("I am an odd number. Take away one letter and I become even. What number am I?", "Seven", listOf("A clock", "Cheese", "The hat gets wet"), 9),
        WordQ("5 5 5 5 5 – Add a symbol somewhere in this row of fives to make an equation equaling 500.", "555-55=500", listOf("Equal", "Island", "A tire"), 10),
        WordQ("If it takes eight men ten hours to build a wall, how long would it take four men?", "Nothing. The wall is already built", listOf("Equal", "Chicago", "Three: A blonde, a brunette, and a redhead"), 6),
        WordQ("If you have a bowl with six apples and you take away four, how many do you have?", "The four you took", listOf("Dozens", "The President", "A friend"), 5),
        WordQ("Forwards I’m heavy, but backward, I’m not. What am I?", "A ton", listOf("A caterpillar", "Playing chess with Kate.", "Seven"), 8),
        WordQ("If you were running a race and passed the person in second place, what place would you be in now?", "Second", listOf("A Kangaroo", "A coin", "A seed"), 8),
        WordQ("What is it that after you take away the whole, some still remains?", "Wholesome", listOf("A weapon", "A peanut", "History"), 9),
        WordQ("Where is 11+2=1?", "In watches", listOf("Dew", "A splinter", "The letter “E”"), 5),
        WordQ("Which two numbers come out the same whether you multiply or add them together?", "2 x 2 = 4, 2 + 2 = 4", listOf("An eight ball", "A coffin", "Sponge"), 8),
        WordQ("What three numbers, none of which is zero, give the same result whether they’re added or multiplied?", "One, two, and three", listOf("Mouth", "A secret", "A sausage"), 5),
        WordQ("If there are three apples and you take away two, how many apples do you have?", "You have two apples", listOf("Concrete floors are hard to crack", "A ton", "Light"), 5),
        WordQ("Mrs. Brown has five daughters. Each of these daughters has a brother. How many children does Mrs. Brown have?", "Six", listOf("Toes", "Address", "A glacier"), 8),
        WordQ("The day before yesterday, I was 21, and next year I will be 24. When is my birthday?", "December 31; today is January 1.", listOf("A doll", "Edam", "A fire"), 8),
        WordQ("A man describes his daughters, saying, “They are all blonde, but two; all brunette but two; and all redheaded but two.” How many daughters does he have?", "Three: A blonde, a brunette, and a redhead", listOf("Letter C", "A doctor", "A ton"), 8),
        WordQ("What is 3/7 chicken, 2/3 cat and 2/4 goat?", "Chicago", listOf("A teabag", "Wholesome", "Letter V"), 10),
        WordQ("If ten birds are sitting in a tree and a hunter shoots one, how many birds are left in the tree?", "None. All birds fly away", listOf("Courage", "A syllable", "A river"), 9),
        WordQ("What bird can lift the most weight?", "A crane", listOf("A friend", "A drum", "A pearl"), 5),
        WordQ("What jumps when it walks and sits when it stands?", "A Kangaroo", listOf("Glass", "Age", "Nothing"), 7),
        WordQ("I am an insect, and the first half of my name reveals another insect. Some famous musicians had a name similar to mine. What am I?", "Beetles", listOf("Canoe", "A rubber band", "Letter “E”"), 5),
        WordQ("Four feet, jagged teeth. Fleet of movement, water, and land. I have no mood; to me you’re food as I drag you under.", "Alligator", listOf("The green elevator", "Dew", "Because it has problems"), 5),
        WordQ("In my life, I die twice, once wrapped in silk, once covered in dust.", "A caterpillar", listOf("A needle", "Mouth", "A drum"), 8),
        WordQ("He’s small, but he can climb a tower.", "An ant", listOf("A road", "A television", "Coconut trees don’t grow bananas."), 6),
        WordQ("Armless, legless, I crawl around when I’m young. Then the time for changing sleep will come. I will awake like a newborn, flying beast,’ till then on the remains of the dead I feast.", "Maggots", listOf("You have two apples", "A drum", "Playing chess with Kate."), 10),
        WordQ("I sit in parliament. You’ll only see me at night. What am I?", "An owl", listOf("A piano", "A rubber band", "A cold"), 5),
        WordQ("What English word retains the same pronunciation, even after you take away four of its five letters?", "Queue", listOf("A rope", "A rubber band", "Stone"), 7),
        WordQ("What occurs once in a minute, twice in a moment, and never in one thousand years?", "The letter M", listOf("Because it has problems", "Parking lot", "An envelope"), 7),
        WordQ("What starts with “e” and ends with “e” but only has one letter in it?", "An envelope", listOf("A cold", "You cannot because the elephant does not have hands.", "The footprints"), 10),
        WordQ("I have seven letters and am something you eat. My only anagram can help your pain. If you remove my first two letters, I wear things down. Removing my first three letters is an adjective and removing my first four letters leaves a measure of time. What am I?", "A sausage", listOf("A cipher key", "Echo", "A piano"), 8),
        WordQ("What word starts with IS, ends with AND, and has LA in the middle?", "Island", listOf("A roof", "A coffin", "Glass"), 9),
        WordQ("I am something many people don’t enjoy having as a friend, including you. But I am called upon anytime someone is injured. I have five letters, and when my last letter is put before my first letter, I become a country. What am I?", "Pains", listOf("A bank", "A teabag", "A violin"), 6),
        WordQ("I’m in cooper but not in a dog. I’m in percent but not in money. What am I?", "Letter C", listOf("Few", "Letter V", "A coffin"), 10),
        WordQ("What four-letter word can be written forward, backward, or upside down and can still be read from left to right?", "Noon", listOf("A seed", "A secret", "Pack of cards"), 7),
        WordQ("I am a three-letter word; add two and fewer there will be. What word am I?", "Few", listOf("A battery", "Corn", "His horse is named Friday"), 9),
        WordQ("What can you find in the middle of “every” but not at the start or end?", "Letter V", listOf("A keyboard", "Letter “R”", "An envelope"), 8),
        WordQ("What always ends everything?", "Letter G", listOf("Flowers", "Queue", "A keyboard"), 9),
        WordQ("What word of five letters has one left when two are removed?", "Stone", listOf("Your name", "Tomorrow", "Beetles"), 7),
        WordQ("Two in a corner, one in a room, zero in a house, but one in a shelter. What is it?", "Letter “R”", listOf("Courage", "Rainbow", "A seed"), 5),
        WordQ("I am a word that begins with the letter “I.” If you add the letter “a” to me, I become a new word with a different meaning, but that sounds exactly the same. What word am I?", "Isle", listOf("Sleep at night.", "A doll", "Carpet"), 9),
        WordQ("What word in the English language does the following: The first two letters signify a male, the first three letters signify a female, the first four letters signify a great, while the entire world signifies a great woman. What is the word?", "Heroine", listOf("They are a grandfather, father, and son.", "A comb", "Coconut trees don’t grow bananas."), 8),
        WordQ("I am the beginning of everything, the end of everywhere. I’m the beginning of eternity, the end of time and space. What am I?", "The letter “E”", listOf("A table", "The library", "Nothing. The wall is already built"), 9),
        WordQ("I am the beginning of sorrow and the end of sickness. You can not express happiness without me, yet I am amid crosses. I am always in risk, yet never in danger. You may find me in the sun, but I am never out of the darkness.", "The letter S", listOf("A compass", "Back to back", "A tire"), 5),
        WordQ("You see me once in June, twice in November, and not at all in May. What am I?", "Letter “E”", listOf("A potato", "Guilt", "A wet one"), 6),
        WordQ("He has one and a person has two, a citizen has three and a human being has four, a personality has five, and an inhabitant of the earth has six. What am I?", "A syllable", listOf("A comb", "Dividend", "Light"), 9),
        WordQ("Which word becomes shorter when you add two letters to it?", "Short", listOf("Dozens", "Corners", "Heroine"), 10),
        WordQ("A word I know, six letters it contains, remove one letter and 12 remains. What is it?", "Dozens", listOf("A secret", "Sleep at night.", "Incorrectly"), 6),
        WordQ("Take away my first letter and I remain the same. Take away my last letter and I remain unchanged. Remove all my letters and I’m still me. What am I?", "A Postman", listOf("Echo", "Sleep at night.", "An arson"), 7),
        WordQ("When is 1500 plus 20 and 1600 minus 40 the same thing?", "Military time", listOf("Corn", "A horse", "Your back"), 7),
        WordQ("I’m where yesterday follows today, and tomorrow is in the middle. What am I?", "Dictionary", listOf("A book", "Equal", "A ton"), 5),
        WordQ("You will always find me in the past. I can be created in the present, But the future can never taint me. What am I?", "History", listOf("Dividend", "Pack of cards", "In watches"), 8),
        WordQ("I have an end but no beginning, a home but no family, a space without a room. I never speak but there is no word I cannot make. What am I?", "A keyboard", listOf("A garbage truck", "An eight ball", "A needle"), 8),
        WordQ("What is a seven letter word that contains thousands of letters?", "Mailbox", listOf("Second", "A fence", "A promise"), 5),
        WordQ("I went to the woods and got it when I got it. I didn’t want it, looked for it, couldn’t find it, so I took it home.", "A splinter", listOf("The green elevator", "Because it has problems", "A Kangaroo"), 10),
        WordQ("What is always coming but never arrives?", "Tomorrow", listOf("A teabag", "Survivors are never buried", "A violin"), 9),
        WordQ("A man notices that his pant pockets are empty. But there is still something to it. What could it be?", "A hole", listOf("A rubber band", "The footprints", "Outside"), 9),
        WordQ("What is the end to which we all like to come?", "Dividend", listOf("A weapon", "Letter “E”", "The footprints"), 8),
        WordQ("There are five sisters in the room. Ann is reading a book, Margaret is cooking, Kate is playing chess, Marie is doing laundry. What is the 5th sister doing?", "Playing chess with Kate.", listOf("None. He has three sisters.", "Dew", "A teabag"), 6),
        WordQ("What is so delicate that even mentioning it breaks it?", "Silence", listOf("The library", "The President", "A syllable"), 7),
        WordQ("What can be broken but never held?", "A promise", listOf("Military time", "Your back", "Short"), 6),
        WordQ("What is it that lives if it is fed and dies if you give it a drink?", "A fire", listOf("Tomorrow", "It sinks", "Lunch and dinner"), 7),
        WordQ("You can have it, and be at it, but it never lasts forever.", "Peace", listOf("Because it has problems", "A coffin", "A hole"), 9),
        WordQ("What can one catch that is not thrown?", "A cold", listOf("Echo", "An envelope", "A stamp"), 10),
        WordQ("What goes up but never ever comes down?", "Age", listOf("Six", "A needle", "A ton"), 9),
        WordQ("It can be cracked, it can be made, it can be told, it can be played. What is it?", "A joke", listOf("Toes", "Outside", "A cold"), 6),
        WordQ("What’s greater than God and more evil than the devil? Rich people want it, and poor people have it. And if you eat it, you’ll die?", "Nothing", listOf("Corners", "The letter S", "Heat because you can always catch a cold."), 6),
        WordQ("I have a thousand wheels, but I do not move. Call me what I am, call me a lot.", "Parking lot", listOf("Letter G", "555-55=500", "One, two, and three"), 5),
        WordQ("Some are quick to take it. Others must be coaxed. Those who choose to take it gain and lose the most.", "Risk", listOf("A secret", "An arson", "A battery"), 7),
        WordQ("What belongs to you but gets used by everyone else more than you?", "Your name", listOf("2 x 2 = 4, 2 + 2 = 4", "A rope", "A saddle"), 7),
        WordQ("People buy me to eat, but never eat me. What am I?", "The plate", listOf("Wisdom", "Mouth", "Nothing"), 8),
        WordQ("Lovely and round, I shine with pale light, Grown in the darkness, a lady’s delight.", "A pearl", listOf("Blame", "A Postman", "Towel"), 5),
        WordQ("I can be as thin as a picture frame, but my insides have many things you can see.", "A television", listOf("Chicago", "2 x 2 = 4, 2 + 2 = 4", "A secret"), 10),
        WordQ("I have teeth but can’t eat. What am I?", "A comb", listOf("Nine", "Beth", "Risk"), 7),
        WordQ("First I am one, then I seem none, in death, I birth a new life. What’s raised exceeds me, for, on bent knees, I add to a world that’s rife. What am I?", "A seed", listOf("Glass", "Sleep at night.", "Rainbow"), 9),
        WordQ("What runs around the yard without moving?", "A fence", listOf("Glass", "A horse", "Age"), 9),
        WordQ("I’m that which is seen only in darkness. Swiftest of all, and near as old as time; Day’s distant brother; fire and faintness, I light without shadow – can you solve this rhyme?", "A starlight", listOf("Tomorrow", "A fire", "An owl"), 8),
        WordQ("Slowly stretching my arms, I rise and move towards warmth. Bursting in colors, my sisters and I. What are we?", "Flowers", listOf("A drum", "Few", "Age"), 8),
        WordQ("We are emeralds and diamonds, lost by the moon, found by the sun, and picked up soon.", "Dew", listOf("Money", "A weapon", "A rubber band"), 10),
        WordQ("The more you take, the more you leave behind. What am I?", "The footprints", listOf("Silence", "Pilgrims", "Dividend"), 10),
        WordQ("What travels the world while stuck in one spot?", "A stamp", listOf("An eraser", "A rubber band", "Towel"), 7),
        WordQ("The one who makes it sells it. The one who buys it doesn’t use it. The one who’s using it doesn’t know he’s using it. What is it?", "A coffin", listOf("The hat gets wet", "Nine", "Incorrectly"), 10),
        WordQ("You throw me out when you need me; you bring me back when you’re done. What am I?", "An anchor", listOf("A promise", "Queue", "Silence"), 10),
        WordQ("If I smile, it also smiles. If I cry, it also cries. If I shout, it does nothing. What is it?", "A mirror", listOf("Risk", "An envelope", "A potato"), 6),
        WordQ("With pointed fangs, I sit and wait, with piercing force, I serve out fate. Grabbing bloodless victims, proclaiming my might; physically joining with a single bite. What am I?", "A stapler", listOf("2 x 2 = 4, 2 + 2 = 4", "A seed", "A coffin"), 9),
        WordQ("My voice is tender, my waist is slender, and I’m often invited to play. Yet wherever I go, I must take my bow, or else I have nothing to say. What am I?", "A violin", listOf("A promise", "Stone", "Rainbow"), 7),
        WordQ("A father’s child, a mother’s child, yet no one’s son?", "A daughter", listOf("A sausage", "Guilt", "The future"), 5),
        WordQ("To give me to someone I don’t belong to is cowardly, but to take me is noble. I can be a game, but nobody wins. What am I?", "Blame", listOf("Six", "Your name", "An arson"), 10),
        WordQ("Tires a horse, worries a man. Tell me this riddle if you can.", "A saddle", listOf("Stone", "Second", "A splinter"), 10),
        WordQ("I cover what is real and hide what is true. But sometimes, I bring out the courage in you. What am I?", "A makeup", listOf("Light", "The footprints", "Letter C"), 8),
        WordQ("A house with two occupants, sometimes one, rarely three. Break the walls, eat the borders, then throw me away. What am I?", "A peanut", listOf("A saddle", "Pack of cards", "Sponge"), 6),
        WordQ("I have three hundred cattle, with a single nose cord.", "Beads", listOf("Darkness", "Flowers", "Pack of cards"), 8),
        WordQ("Although I’m far from the point, I’m not a mistake. I fix yours. What am I?", "An eraser", listOf("The hat gets wet", "The President", "Peace"), 6),
        WordQ("Hands she has but does not hold, teeth she has but does not bite, feet she has but they are cold, eyes she has but without sight. Who is she?", "A doll", listOf("Address", "A book", "A rubber band"), 5),
        WordQ("What is bought by the yard is worn by the foot?", "Carpet", listOf("A television", "A map", "A piano"), 7),
        WordQ("The restraining hand. It keeps us from doing horrible things, and it is hard to live with. What is it?", "Guilt", listOf("Soul", "Courtship", "A crane"), 9),
        WordQ("What can’t you see that is always before you?", "The future", listOf("Risk", "Carpet", "A piano"), 6),
        WordQ("What is gold when old and silver when new, hard to find but easy to lose, cost a lot, but it’s free?", "A friend", listOf("Lunch and dinner", "Parking lot", "Because it has problems"), 8),
        WordQ("Poorly behaved children often find themselves sitting in these.", "Corners", listOf("A glove", "A rope", "Dictionary"), 10),
        WordQ("A natural state, I’m sought by all. Go with me, and you shall fall. You do me when you spend, and you use me when you eat to no end. What am I?", "Balance", listOf("A mirror", "A cold", "A coin"), 9),
        WordQ("I cannot be felt, seen, or touched; yet I can be found in everybody. My existence is always in debate, yet there is a style of music named after me.", "Soul", listOf("Beads", "A peanut", "A garbage truck"), 10),
        WordQ("I am rather large and usually majestic. I am every hue of the rainbow. I can eat you, I may heat you. You only wish you could see me. What am I?", "Dragon", listOf("A road", "Money", "An eraser"), 5),
        WordQ("I work hard almost every day, not much time to dance and play. If I could reach what I desire, all like me would now retire. What am I?", "A doctor", listOf("A sausage", "A rope", "Beads"), 10),
        WordQ("Ten men’s strength, ten men’s length, ten men can’t break it, yet a young boy walks off with it. What am I?", "A rope", listOf("A tie", "Outside", "A bank"), 5),
        WordQ("I am nothing but holes tied to holes, yet I am strong as iron.", "A chain", listOf("Silence", "A ton", "A kite"), 9),
        WordQ("Blow for blow, they matched each other. Neither would fall to the other. In the eyes of the crowd, they were this.", "Equal", listOf("Glass", "An ant", "A secret"), 9),
        WordQ("I have lasted many years and still feel young. I have endured depressions, recessions, and even millenniums. I’m richer than the richest of men. You can visit me, but not my owners. I’ve been shown on TV, and I can take and give you what is yours, but only if you ask me to. So tell me who or what I am?", "A bank", listOf("Sleep at night.", "Dividend", "Risk"), 6),
        WordQ("You can only have it once you have given it.", "Respect", listOf("Nothing", "A hole", "Silence"), 5),
        WordQ("Many have heard me, but nobody has seen me, and I will not speak back until spoken to. What am I?", "Echo", listOf("Concrete floors are hard to crack", "A snowman", "A needle"), 5),
        WordQ("What falls but never breaks and breaks but never falls?", "Night and day", listOf("A splinter", "A promise", "A pearl"), 9),
        WordQ("I’m so simple I only point, yet I guide people all over the world. What am I?", "A compass", listOf("Chicago", "Your name", "Letter G"), 10),
        WordQ("My step is slow, the snow’s my breath. I give the ground a grinding. My marching makes an end of me slain by the sun or drowned in the sea.", "A glacier", listOf("A kite", "Address", "Night and day"), 7),
        WordQ("I am one of many, you forget I’m here, but I’m just below without me, you’d surely topple. Go ahead, what am I?", "Toes", listOf("A caterpillar", "A doll", "A priest"), 7),
        WordQ("I go in dry and come out wet; the longer I’m in, the stronger I get. What am I?", "A teabag", listOf("A wet one", "A rubber band", "A clock"), 7),
        WordQ("I am born in fear, raised in the truth, and I come to my own in deed. When comes a time that I’m called forth, I come to serve the cause of need.", "Courage", listOf("Night and day", "A doctor", "Seven"), 10),
        WordQ("My first is in the wield, sever bones, and marrow. My second is in the blade, forged in cold steel. My third is in arbalest, and also in arrows. My fourth is in power, plunged through a shield. My fifth is in honor, and also in vows. My last will put an end to it all.", "A weapon", listOf("A coffin", "A garbage truck", "Letter G"), 5),
        WordQ("There are two meanings to me. With one, I may need to be broken. With the other, I hold on. My favorite characteristic is my charming dimple. What am I?", "A tie", listOf("An arson", "A sausage", "A doctor"), 10),
        WordQ("You can easily touch me but not see me. You can throw me out, but not away. What am I?", "Your back", listOf("An eraser", "Risk", "Chicago"), 9),
        WordQ("If you live in an 11-floor house and everything is green, the home, blender, toilet, elevator, ribbon, couch, computer, plates, food! You get to the 6th floor, and there are no more stairs. How did you get to the 11th floor?", "The green elevator", listOf("Courage", "You have two apples", "An eraser"), 6),
        WordQ("Not born, but from a Mother’s body drawn, I hang until half of me is gone. I sleep in a cave until I grow old, then valued for my hardened gold. What am I?", "Cheese", listOf("Dozens", "A battery", "The letter M"), 7),
        WordQ("Break it and it is better, immediately set, and harder to break again.", "A record", listOf("A chalkboard", "Yarn", "Dividend"), 8),
        WordQ("If you have me, you want to tell me. If you tell me, you don’t have me.", "A secret", listOf("Short", "Beth", "Nothing. The wall is already built"), 9),
        WordQ("Born of sorrow, grows with age, you need a lot to be a sage. What is it?", "Wisdom", listOf("History", "Respect", "It sinks"), 6),
        WordQ("To unravel me, you need a key. No key that was made by locksmith’s hand, but a key that only I will understand. What am I?", "A cipher key", listOf("Dragon", "Stone", "Age"), 6),
        WordQ("I have branches, but no fruit, trunk, or leaves. What am I?", "A bank", listOf("None. All birds fly away", "Add a ‘G,’ and it’s gone!", "A fence"), 7),
        WordQ("The more of this there is, the less you see. What is it?", "Darkness", listOf("Military time", "A palm tree", "Carpet"), 8),
        WordQ("I’m light as a feather, yet the strongest person can’t hold me for five minutes. What am I?", "Breath", listOf("Address", "Soul", "The footprints"), 9),
        WordQ("I’m found in socks, scarves, and mittens; and often in the paws of playful kittens. What am I?", "Yarn", listOf("Light", "Heat because you can always catch a cold.", "2 x 2 = 4, 2 + 2 = 4"), 7),
        WordQ("What can run but never walks, has a mouth but never talks, has a head but never weeps, has a bed but never sleeps?", "A river", listOf("Chicago", "Nothing. The wall is already built", "Elephant’s shadow"), 5),
        WordQ("What can fill a room but takes up no space?", "Light", listOf("Dew", "Seven", "The hat gets wet"), 8),
        WordQ("People make me, save me, change me, raise me. What am I?", "Money", listOf("Corn", "Letter C", "Nothing"), 5),
        WordQ("What goes through cities and fields but never moves?", "A road", listOf("Beads", "Breath", "Dozens"), 5),
        WordQ("I have lakes with no water, mountains with no stone, and cities with no buildings. What am I?", "A map", listOf("Edam", "Six", "The letter S"), 5),
        // Ages 3-5
        WordQ("What has a face and two hands but no arms or legs?", "Clock", listOf("Robot", "Tree", "Table"), 4),
        WordQ("What has an eye but cannot see?", "Needle", listOf("Potato", "Storm", "Bat"), 5),
        
        // Ages 6-8
        WordQ("What type of dress can never be worn?", "Address", listOf("Uniform", "Suit", "Costume"), 6),
        WordQ("What is full of holes but still can hold water?", "Sponge", listOf("Bucket", "Cup", "Net"), 6),
        WordQ("What goes up but never comes back down?", "Your age", listOf("Balloon", "Rocket", "Bird"), 7),
        WordQ("What has many keys but can't open a door?", "Piano", listOf("Locksmith", "Keyboard", "Safe"), 7),
        WordQ("What gets bigger the more you take away?", "A hole", listOf("A balloon", "A cloud", "A puzzle"), 8),
        
        // Ages 9-10
        WordQ("Two fathers and two sons are in a car, yet there are only three people in the car. How?", "Grandfather, father, and son", listOf("One is a ghost", "They are twins", "They are brothers"), 9),
        WordQ("What belongs to you but gets used by everyone else more than you?", "Your name", listOf("Your phone", "Your money", "Your car"), 9),
        WordQ("How can you go 25 days without sleep?", "Sleep at night", listOf("Drink coffee", "Stay awake", "Impossible"), 10),
        WordQ("What moves faster: heat or cold?", "Heat (you catch a cold)", listOf("Cold", "Neither", "Light"), 10),
        WordQ("What word is spelled incorrectly in every single dictionary?", "Incorrectly", listOf("Misspelled", "Wrongly", "Alphabet"), 10)
    )

    private fun generateTrickyChallenge(age: Int): Challenge {
        var suitable = trickyList.filter { age - it.minAge in 0..2 }
        if (suitable.isEmpty()) {
            suitable = trickyList.filter { it.minAge <= age }
        }
        val item = if(suitable.isNotEmpty()) suitable.random() else trickyList.maxByOrNull { it.minAge }!!
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
