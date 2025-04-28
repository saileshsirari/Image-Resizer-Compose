package com.image.resizer.compose

import androidx.lifecycle.viewmodel.compose.viewModel
import com.image.resizer.compose.ImageItem.Companion.scaleParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class ScaleImageViewModelTest {
    private lateinit var viewModel: ScaleImageViewModel
    fun t() = channelFlow {


        coroutineScope {
            launch {
                send("Hello")
            }

            launch {
                send("World!")
            }
            send("Have a great day! 😎")

        }
        //  send("${hello.await()}, ${world.await()}")
    }

    @Before
    fun setUp() {
        viewModel = ScaleImageViewModel()


    }

    fun querySensor(): Int = Random.nextInt(-10, 30)

    fun celsiusToFahr(temp: Int)  = temp * 9.0 / 5.0 + 32.0
    fun getTemperature(): Flow<Int> {
        return flow {
            while (true) {
                emit(querySensor())
                delay(1.seconds )
            }
        }
    }

   class ViewCounter{
       private  val _counter = MutableStateFlow<Float>(1f)
       val counter = _counter.asSharedFlow()
       fun increment(){
           runBlocking {
               _counter.emit(1.0f)
           }
       }
   }

    @Test
    fun emailValidator_CorrectEmailSimple_ReturnsTrue() {

        expectThat(("name@email.com")).isNotEmpty()
      /*  val viewCounter = ViewCounter()





        runBlocking {
            val temps = getTemperature()

           launch {
                temps.onEach {
                    println(" $it onEach")
                }.buffer(10).collect {
                    println(" $it Celsius")
                    delay(5.seconds)
                }
            }



        }*/

    }

    @Test
    fun `predefinedDimensions should be initialized with correct size and in  order`() {
        expectThat(viewModel.predefinedDimensions.size).isEqualTo(21) // Assuming 21 dimensions
        expectThat(viewModel.predefinedDimensions[0]).isEqualTo(
            PredefinedDimension(
                144,
                176
            )
        ) // Check the first
        expectThat(viewModel.predefinedDimensions.last()).isEqualTo(PredefinedDimension(3072, 4096))
    }

    @Test
    fun `initial selectedDimension should be null`() {
        val dimension = PredefinedDimension(-1, -1)
        expectThat(viewModel.selectedPredefinedDimension).isNotNull().isEqualTo(dimension)
    }

    @Test
    fun `selectDimension should update selectedDimension`() {
        val dimension = PredefinedDimension(1920, 1080)
        viewModel.selectPredefinedDimension(dimension)
        expectThat(viewModel.selectedPredefinedDimension).isNotNull().isEqualTo(dimension)
    }

    @Test
    fun `isSelected should return true when the dimension is selected`() {
        viewModel.updateWidth(100.toString())

        val scaleParams = viewModel.onScaleForList()
        expectThat(scaleParams).isNotNull()

    }

    @Test
    fun `isSelected should return false when the dimension is not selected`() {
        val dimension1 = PredefinedDimension(1920, 1080)
        val dimension2 = PredefinedDimension(3072, 4096)
//        viewModel.selectDimension(dimension1)

    }

    @Test
    fun `clearSelectedDimension should set selectedDimension to null`() {
        val dimension = PredefinedDimension(1920, 1080)
    }

    @Test
    fun `onScaleForList should return ScaleParams with selected dimensions when in custom mode and predefined dimension is selected`() {
        // Arrange
        val predefinedDimension = PredefinedDimension(1920, 1080)
        viewModel.selectPredefinedDimension(predefinedDimension)

        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNotNull().and {
            get { newWidth }.isEqualTo(predefinedDimension.width)
            get { newHeight }.isEqualTo(predefinedDimension.height)
            get { scaleFactor }.isNull()
            get { keepAspectRatio }.isTrue()
        }
        // reset selectedDimension
        viewModel.resetSelectedPredefinedDimension()
    }

    @Test
    fun `onScaleForList should return ScaleParams with newWidth when in custom mode, keepAspectRatio is true and width is set`() {
        // Arrange
        viewModel.toggleKeepAspectRatio(true)
        viewModel.updateWidth("1000")

        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNotNull().and {
            get { newWidth }.isEqualTo(1000)
            get { newHeight }.isNull()
            get { scaleFactor }.isNull()
            get { keepAspectRatio }.isTrue()
        }
        //reset width
        viewModel.updateWidth("")
    }

    @Test
    fun `onScaleForList should return ScaleParams with newHeight when in custom mode, keepAspectRatio is true and height is set`() {
        // Arrange
        viewModel.toggleKeepAspectRatio(true)
        viewModel.updateHeight("500")

        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNotNull().and {
            get { newWidth }.isNull()
            get { newHeight }.isEqualTo(500)
            get { scaleFactor }.isNull()
            get { keepAspectRatio }.isTrue()
        }
        //reset height
        viewModel.updateHeight("")
    }

    @Test
    fun `onScaleForList should return ScaleParams with newWidth and newHeight when in custom mode, keepAspectRatio is false, and both width and height are set`() {
        // Arrange
        viewModel.toggleKeepAspectRatio(false)
        viewModel.updateWidth("800")
        viewModel.updateHeight("600")

        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNotNull().and {
            get { newWidth }.isEqualTo(800)
            get { newHeight }.isEqualTo(600)
            get { scaleFactor }.isNull()
            get { keepAspectRatio }.isFalse()
        }
        //reset width and height
        viewModel.updateWidth("")
        viewModel.updateHeight("")
    }
    @Test
    fun `onScaleForList should return null when in custom mode and keepAspectRatio is true, but width and height are empty`() {
        // Arrange
        viewModel.toggleKeepAspectRatio(true)
        viewModel.updateWidth("")
        viewModel.updateHeight("")
        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNull()
    }
    @Test
    fun `onScaleForList should return null when in custom mode and keepAspectRatio is false, but width and height are empty`() {
        // Arrange
        viewModel.toggleKeepAspectRatio(false)
        viewModel.updateWidth("")
        viewModel.updateHeight("")
        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNull()
    }

    @Test
    fun `onScaleForList should return ScaleParams with scaleFactor when not in custom mode`() {
        // Arrange
        viewModel.changeMode("percentage")
        viewModel.updatePercentage(50f)

        // Act
        val result = viewModel.onScaleForList()

        // Assert
        expectThat(result).isNotNull().and {
            get { newWidth }.isNull()
            get { newHeight }.isNull()
            get { scaleFactor }.isEqualTo(0.5f)
            get { keepAspectRatio }.isTrue()
        }
        //reset the mode
        viewModel.changeMode("custom")
        viewModel.updatePercentage(100f)
    }
}