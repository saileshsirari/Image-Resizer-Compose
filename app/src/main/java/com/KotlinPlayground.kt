package com

class KotlinPlayground {
    sealed class Fruit{
        abstract val weight:Int
    }
    data class Apple(override val weight: Int, val color:String) : Fruit( )
    data class Orange(override val weight: Int, val juicy: Boolean) : Fruit( )
    fun < T>copyData(source: MutableList< T>,destination: MutableList< T>): List<T>{
        for (item in source){
            destination.add(item)
        }
        return  destination
    }
    fun  compareFruits(){
        val weightComparator = Comparator<Fruit>{ fruit1, fruit2 ->
            fruit1.weight -fruit2.weight
        }
        val fruits = listOf<Fruit>(Orange(180,true), Apple(120,"Green"))
        val apples = listOf<Apple>(Apple(20,"red"), Apple(12,"green"))
        println(fruits.sortedWith( weightComparator))
        val a  = copyData<Fruit>(apples.toMutableList(),fruits.toMutableList())

        println(apples.sortedWith (weightComparator) )
        println("here $a")

    }
}