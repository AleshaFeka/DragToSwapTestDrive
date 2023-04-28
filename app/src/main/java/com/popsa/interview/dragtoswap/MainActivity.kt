package com.popsa.interview.dragtoswap

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.popsa.interview.dragtoswap.MainActivityCoordinator.Events.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_scrolling.*

private fun ImageView.changeDarknessSmoothly(isDark: Boolean) {
    if (isDark) {
        ObjectAnimator.ofArgb(this, "colorFilter", 0x00000000, 0x70000000)
    } else {
        ObjectAnimator.ofArgb(this, "colorFilter", 0x70000000, 0x00000000)
    }.apply {
        duration = 250
    }.start()
}

/**
 * Place for applying view data to views, and passing actions to coordinator
 */

private const val DRAGGABLE_ITEM_SIZE = 50

class MainActivity : AppCompatActivity() {


    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var coordinator: MainActivityCoordinator

    // Should be stored in ViewModel and observed properly...
    private var dragImageIndex: Int = -1

    // The same
    private val blendedImagesState: Array<Boolean> = Array(4) { false }

    private val imageViews: List<ImageView> by lazy { listOf(image1, image2, image3, image4) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coordinator = MainActivityCoordinator(viewModel)
        setSupportActionBar(toolbar)
        toolbar.title = title

        viewModel.images.observe(this, Observer { images ->
            // Load all the images from the viewModel into ImageViews
            imageViews.forEachIndexed { index, imageView ->
                Glide.with(this).load(images[index].imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(400)).into(imageView)
                imageView.tag =
                    index // Quick&dirty: stash the index of this image in the ImageView tag
            }
        })

        list.setOnTouchListener { _, event ->
            val eventX = event.x.toInt()
            val eventY = event.y.toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> { // Hunt for what's under the drag and start dragging
                    getImageViewAt(eventX, eventY)?.let {
                        val index = it.tag as Int
                        coordinator.startedSwap(index, eventX, eventY)
                    }
                }

                MotionEvent.ACTION_UP -> { // If we are dragging to something valid, do the swap
                    coordinator.imageDropped(eventX, eventY)
                }

                MotionEvent.ACTION_MOVE -> {
                    coordinator.imageDragInProgress(eventX, eventY)
                }
            }
            true
        }

        viewModel.events.observe(this, Observer {
            when (it) {
                is ImageDropped -> dropImage(it.x, it.y)
                is ImageDragInProgress -> proceedImageDrag(it.x, it.y)
                is ImageSelected -> proceedImageTouchDown(it.x, it.y)
            }
        })
    }

    private fun proceedImageDrag(eventX: Int, eventY: Int) {
        updateImagesDarkness {
            val image = getImageViewAt(eventX, eventY)
            if (image != null) {
                blendedImagesState.fill(false)
                blendedImagesState[dragImageIndex] = true
                blendedImagesState[image.tag as Int] = true
            }
        }

        moveDraggable(eventX, eventY)
    }

    private fun proceedImageTouchUd() {
        draggable.visibility = View.GONE
        updateImagesDarkness {
            blendedImagesState.fill(false)
            dragImageIndex = -1
        }
    }

    private fun proceedImageTouchDown(eventX: Int, eventY: Int) {
        val image = getImageViewAt(eventX, eventY)
        updateImagesDarkness {
            val index = image?.tag as Int
            dragImageIndex = index
            blendedImagesState[index] = true
        }

        draggable.setImageBitmap(
            image?.drawable?.toBitmap(
                DRAGGABLE_ITEM_SIZE,
                DRAGGABLE_ITEM_SIZE
            )
        )
        draggable.visibility = View.VISIBLE
        moveDraggable(eventX, eventY)
    }

    private fun moveDraggable(eventX: Int, eventY: Int) {
        draggable.animate()
            .x(eventX - draggable.width / 2f)
            .y(eventY - draggable.height / 2f)
            .setDuration(0).start()
    }

    private fun updateImagesDarkness(block: () -> Unit) {
        val oldBlendedImagesState = blendedImagesState.clone()

        block()

        if (!blendedImagesState.contentEquals(oldBlendedImagesState)) {
            updateBlends(oldBlendedImagesState)
        }
    }

    private fun updateBlends(oldBlendedImagesState: Array<Boolean>) {
        imageViews.forEachIndexed { index, imageView ->
            if (oldBlendedImagesState[index] != blendedImagesState[index])
                imageView.changeDarknessSmoothly(isDark = blendedImagesState[index])
        }
    }

    private fun dropImage(eventX: Int, eventY: Int) {
        val sourceImageIndex = viewModel.draggingIndex.value
        val targetImage = getImageViewAt(eventX, eventY)
        val targetImageIndex = targetImage?.let { it.tag as Int }
        if (targetImageIndex != null && sourceImageIndex != null && targetImageIndex != sourceImageIndex) coordinator.swapImages(
            sourceImageIndex,
            targetImageIndex
        )
        else coordinator.cancelSwap()
        proceedImageTouchUd()
    }

    private fun getImageViewAt(x: Int, y: Int): ImageView? {
        val hitRect = Rect()
        return imageViews.firstOrNull {
            it.getHitRect(hitRect)
            hitRect.contains(x, y)
        }
    }

}