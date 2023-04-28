package com.popsa.interview.dragtoswap

/**
 * Place for logic
 */
class MainActivityCoordinator(
    private val viewModel: MainActivityViewModel
) {

    private val imageRepository = FakeDi.imageRepository

    init {
        viewModel.images.value = imageRepository.list
    }

    fun swapImages(position1: Int, position2: Int) {
        val item1 = imageRepository.list[position1]
        val item2 = imageRepository.list[position2]
        imageRepository.list[position1] = item2
        imageRepository.list[position2] = item1
        viewModel.images.value = imageRepository.list
        cancelSwap()
    }

    fun startedSwap(index: Int, eventX: Int, eventY: Int) {
        viewModel.draggingIndex.value = index
        viewModel.events.value = Events.ImageSelected(eventX, eventY)
    }

    fun imageDragInProgress(eventX: Int, eventY: Int) {
        viewModel.events.value = Events.ImageDragInProgress(eventX, eventY)
    }

    fun cancelSwap() {
        viewModel.draggingIndex.value = null
    }

    fun imageDropped(eventX: Int, eventY: Int) {
        viewModel.events.value = Events.ImageDropped(eventX, eventY)
    }

    sealed class Events {
        data class ImageDropped(val x: Int, val y: Int) : Events()
        data class ImageDragInProgress(val x: Int, val y: Int) : Events()
        data class ImageSelected(val x: Int, val y: Int) : Events()
    }
}