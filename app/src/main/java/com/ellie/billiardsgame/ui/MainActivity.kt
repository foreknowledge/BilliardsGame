package com.ellie.billiardsgame.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ellie.billiardsgame.BilliardsMode
import com.ellie.billiardsgame.R
import com.ellie.billiardsgame.customview.BallView
import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("ClickableViewAccessibility")
class MainActivity : AppCompatActivity() {
    private val readyModeActionConductor = ReadyModeActionConductor()
    private val editModeActionConductor = EditModeActionConductor()
    private val executeModeActionConductor = ExecuteModeActionConductor()

    private var modeActionConductor: ModeActionConductor = readyModeActionConductor

    private val mainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithNoStatusBar()
        addGlobalLayoutListener()

        subscribeUI()
        setViewListeners()
    }

    private fun setContentViewWithNoStatusBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)
    }

    private fun addGlobalLayoutListener() {
        parentLayout.viewTreeObserver.addOnGlobalLayoutListener {
            initDataInViewModel()
        }
    }

    private fun initDataInViewModel() {
        mainViewModel.apply {
            whiteBall.update(whiteBallView.x, whiteBallView.y)
            redBall1.update(redBallView1.x, redBallView1.y)
            redBall2.update(redBallView2.x, redBallView2.y)
            setBoundary(poolTableView.top, poolTableView.right, poolTableView.bottom, poolTableView.left)
        }
    }

    private fun subscribeUI() = with(mainViewModel) {
        val owner = this@MainActivity
        whiteBall.point.observe(owner, Observer {
            whiteBallView.x = it.x
            whiteBallView.y = it.y
        })
        redBall1.point.observe(owner, Observer {
            redBallView1.x = it.x
            redBallView1.y = it.y
        })
        redBall2.point.observe(owner, Observer {
            redBallView2.x = it.x
            redBallView2.y = it.y
        })
        curMode.observe(owner, Observer { applyChangedMode(it) })
    }

    private fun applyChangedMode(mode: BilliardsMode) {
        modeActionConductor = when (mode) {
            BilliardsMode.READY -> readyModeActionConductor
            BilliardsMode.EDIT -> editModeActionConductor
            BilliardsMode.EXECUTE -> executeModeActionConductor
        }

        button.text = modeActionConductor.btnText
        lineCanvas.removeLine()
    }

    private fun setViewListeners() {
        setWhiteBallTouchListener()
        setRedBallTouchListener()
        setButtonClickListener()
    }

    private fun setWhiteBallTouchListener() {
        whiteBallView.setOnTouchListener { v, event ->
            modeActionConductor.onWhiteBallTouch(event)
        }
    }

    private fun setRedBallTouchListener() {
        redBallView1.setOnTouchListener { v, event ->
            modeActionConductor.onRedBallTouch(v as BallView, event)
        }

        redBallView2.setOnTouchListener { v, event ->
            modeActionConductor.onRedBallTouch(v as BallView, event)
        }
    }

    private fun setButtonClickListener() {
        button.setOnClickListener {
            modeActionConductor.onButtonClick()
        }
    }

    interface ModeActionConductor {
        val btnText: String
        fun onWhiteBallTouch(event: MotionEvent): Boolean
        fun onRedBallTouch(ballView: BallView, event: MotionEvent): Boolean
        fun onButtonClick()
    }

    inner class ReadyModeActionConductor : ModeActionConductor {
        override val btnText: String by lazy { this@MainActivity.getText(R.string.btn_shot).toString() }

        private val gestureDetector by lazy {
            GestureDetectorCompat(this@MainActivity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent?) {
                    mainViewModel.changeMode(BilliardsMode.EDIT)
                }
            })
        }

        override fun onRedBallTouch(ballView: BallView, event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)

            return true
        }

        override fun onWhiteBallTouch(event: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(event)
            lineCanvas.drawLine(whiteBallView.centerX, whiteBallView.centerY, event.rawX, event.rawY)

            return true
        }

        override fun onButtonClick() {
            val velocity = lineCanvas.line.getVelocity()

            if (velocity.x * velocity.y != 0f) {
                mainViewModel.startSimulation(velocity)
                mainViewModel.changeMode(BilliardsMode.EXECUTE)
            }
        }
    }

    inner class EditModeActionConductor : ModeActionConductor {
        override val btnText: String by lazy { this@MainActivity.getText(R.string.btn_ok).toString() }

        override fun onWhiteBallTouch(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_MOVE) {
                val x = event.rawX - whiteBallView.radius
                val y = event.rawY - whiteBallView.radius

                with(mainViewModel) {
                    updatePositionByApplyingCollision(whiteBall, x, y)
                }
            }

            return true
        }

        override fun onRedBallTouch(ballView: BallView, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_MOVE) {
                val x = event.rawX - ballView.radius
                val y = event.rawY - ballView.radius

                with(mainViewModel) {
                    if (ballView.id == R.id.redBallView1) {
                        updatePositionByApplyingCollision(redBall1, x, y)
                    } else {
                        updatePositionByApplyingCollision(redBall2, x, y)
                    }
                }
            }

            return true
        }

        override fun onButtonClick() {
            mainViewModel.changeMode(BilliardsMode.READY)
        }
    }

    inner class ExecuteModeActionConductor : ModeActionConductor {
        override val btnText: String by lazy { this@MainActivity.getText(R.string.btn_end).toString() }

        override fun onWhiteBallTouch(event: MotionEvent) = false

        override fun onRedBallTouch(ballView: BallView, event: MotionEvent) = false

        override fun onButtonClick() {
            mainViewModel.stopSimulation()
            mainViewModel.changeMode(BilliardsMode.READY)
        }
    }
}