package com.meteors.android.dotcraftdemo

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.meteors.android.dotcraftdemo.databinding.ActivityMainBinding
import com.meteors.android.dotcraftdemo.databinding.LayoutAlertDialogBinding
import kotlin.collections.ArrayList

private const val TAG = "MainActivity"

/**
 * MainActivity将游戏布局托管给Fragment
 * MainActivity负责游戏逻辑的判断、计时、记录成绩、关卡选择、刷新的功能实现
 * 抽象类BaseFragment为中间的棋盘布局，实现布局的加载，提供布局初始化、获得View引用的接口
 * 从Fragment获取视图引用必须使用LifeCircle监听Fragment的生命周期，以免Fragment未加载完成获得空引用
 * FragmentLevel3和FragmentLevel4分别为3x3、4x4的游戏布局，实现了BaseFragment的接口
 */
class MainActivity : AppCompatActivity(), LifecycleObserver {
    private lateinit var binding: ActivityMainBinding       //整个布局的ViewBinding

    private var level = 3               //游戏布局为level x level: 3x3和4x4
    private var ringNum = 3             //ringNum为圆环Ring的数量

    private lateinit var containerViewArr: List<ImageView>          //Ring的View引用
    private lateinit var dotViewArr: List<ImageView>                //Dot的View引用
    private lateinit var containerArr: Array<Int?>                  //Ring的位置
    private lateinit var dotArr: Array<Int?>                        //Dot的位置

    private lateinit var fragment: BaseFragment         //引用布局中的Fragment

    private lateinit var sp: SharedPreferences          //SharePreference记录成绩

    private lateinit var backupDot: ImageView

    private var lastMotionX = 0f            //触摸点坐标
    private var lastMotionY = 0f
    private var touchIndex = 0

    private var state = STATE_IDLE          //处理机状态

    private var touchSlop = 0

    private var startTime: Long = 0         //记录时间
    private var endTime: Long = 0

    companion object {
        private const val STATE_IDLE = 0            //状态机常量
        private const val STATE_WAITING_DRAG = 1
        private const val STATE_HORIZONTAL_DRAG = 2
        private const val STATE_VERTICAL_DRAG = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)
        binding.toolbarText.text = "· Level $level ·"
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //向Container中添加Fragment，初始化为3x3的Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container_layout)
        if (currentFragment == null) {
            fragment = FragmentLevel3()
            fragment.lifecycle.addObserver(this)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.container_layout, fragment)
                .commit()
        }

        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        sp = getSharedPreferences("gameRecord", MODE_PRIVATE)

        //timer为Chronometer计时器组件，设置基准，开始计时
        binding.timer.apply {
            startTime = SystemClock.elapsedRealtime()
            base = startTime
            start()
        }
    }

    /**
     * 观察Fragment的生命周期，当视图创建完毕后再向Fragment获取Dot和Ring的引用
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun fragmentOnStart() {
        fragment.initLevel(ringNum)
        dotArr = fragment.dotArr
        dotViewArr = fragment.getDotViewArray()
        containerArr = fragment.containerArr
        containerViewArr = fragment.getContainerViewArray()
        backupDot = fragment.getBackupDot()
    }

    //加载菜单项
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    /**
     * 为ToolBar的工具栏个按钮设置事件监听，包括刷新、查看游戏记录、切换关卡
     * 关卡3x3分为Dot个数为3、4、5
     * 关卡4x4分为Dot个数为5、6、7、8
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.restart -> {
                initLevel(level, ringNum)
                true
            }
            R.id.record -> {
                showRecord()
                true
            }
            R.id.level1_1 -> {
                initLevel(3, 3)
                true
            }
            R.id.level1_2 -> {
                initLevel(3, 4)
                true
            }
            R.id.level1_3 -> {
                initLevel(3, 5)
                true
            }
            R.id.level2_1 -> {
                initLevel(4, 5)
                true
            }
            R.id.level2_2 -> {
                initLevel(4, 6)
                true
            }
            R.id.level2_3 -> {
                initLevel(4, 7)
                true
            }
            R.id.level2_4 -> {
                initLevel(4, 8)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    /**
     * 使用SharedPreference查询成绩记录，包括关卡是否完成，最佳成绩等
     * 键success为关卡完成记录；键record为事件记录
     * 使用AlertDialog，自定义布局进行展示
     */
    private fun showRecord() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        val dialogBinding = LayoutAlertDialogBinding.inflate(layoutInflater)

        dialog.setView(dialogBinding.root)
        val successList = listOf(
            dialogBinding.success11,
            dialogBinding.success12,
            dialogBinding.success13,
            dialogBinding.success21,
            dialogBinding.success22,
            dialogBinding.success23,
            dialogBinding.success24
        )
        val recordList = listOf(
            dialogBinding.record11,
            dialogBinding.record12,
            dialogBinding.record13,
            dialogBinding.record21,
            dialogBinding.record22,
            dialogBinding.record23,
            dialogBinding.record24
        )
        var count = 0
        var l = 3
        for (i in 3..5) {
            if (sp.getBoolean("success$l-$i", false)) {
                val bestTime = sp.getString("record$l-$i", "")
                successList[count].isChecked = true
                recordList[count].apply {
                    visibility = View.VISIBLE
                    text = "${this.text}${bestTime}s"
                }
            }
            ++count
        }
        l = 4
        for (i in 5..8) {
            if (sp.getBoolean("success$l-$i", false)) {
                val bestTime = sp.getString("record$l-$i", "")
                successList[count].isChecked = true
                recordList[count].apply {
                    visibility = View.VISIBLE
                    text = "${this.text}${bestTime}s"
                }
            }
            ++count
        }
        dialog.show()
    }

    /**
     * 切换关卡，3x3和4x4之间需要进行Fragment的替换，并重新计时
     */
    private fun initLevel(newLevel: Int, newRingNum: Int) {
        if (level != newLevel) {
            fragment.lifecycle.removeObserver(this)         //释放生命周期观察
            when (newLevel) {
                3 -> {
                    level = newLevel
                    ringNum = newRingNum
                    fragment = FragmentLevel3()
                    fragment.lifecycle.addObserver(this)        //对新Fragment进行生命周期监听
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container_layout, fragment)
                        .commit()          //开启事务进行Fragment替换
                }
                4 -> {
                    level = newLevel
                    ringNum = newRingNum
                    fragment = FragmentLevel4()
                    fragment.lifecycle.addObserver(this)
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container_layout, fragment)
                        .commit()
                }
            }
        } else {
            ringNum = newRingNum
            fragment.initLevel(ringNum)
        }
        binding.timer.apply {
            startTime = SystemClock.elapsedRealtime()
            base = startTime
            start()
        }
        binding.toolbarText.text = "· Level $level ·"
    }

    /**
     * 触摸事件监听
     * ACTION_DOWN触摸事件；ACTION_MOVE滑动事件；ACTION_UP手指抬起事件
     * 需要完成点击是否有效、滑动距离、滑动方向的判断
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                state = STATE_IDLE
                touchIndex = -1
                lastMotionX = event.rawX
                lastMotionY = event.rawY
                var i = 0                               //遍历所有布局，判断点击是否有效
                while (i < level * level) {
                    val dotView = dotViewArr[i]
                    if (isPointInView(dotView, lastMotionX, lastMotionY)) {
                        touchIndex = i                  //触摸坐标有效，记录点击的View索引
                        state = STATE_WAITING_DRAG      //状态机切换为为WAITING
                        break
                    }
                    i++
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastMotionX
                val deltaY = event.rawY - lastMotionY
                if (state == STATE_WAITING_DRAG) {              // 超过一定距离才认为有效滑动，抖动处理
                    if (Math.abs(deltaX) >= touchSlop || Math.abs(deltaY) >= touchSlop) {
                        state =
                            if (Math.abs(deltaX) > Math.abs(deltaY)) STATE_HORIZONTAL_DRAG else STATE_VERTICAL_DRAG
                    }
                }
                if (state == STATE_HORIZONTAL_DRAG) {
                    horizontalDragging(touchIndex / level, deltaX)      // 横向滑动， 传入行坐标
                } else if (state == STATE_VERTICAL_DRAG) {
                    verticalDragging(touchIndex % level, deltaY)        // 纵向滑动， 传入列坐标
                }
                lastMotionX = event.rawX        //更新最新坐标
                lastMotionY = event.rawY
            }

            MotionEvent.ACTION_UP -> {
                if (state == STATE_HORIZONTAL_DRAG) {
                    horizontalDragEnd(touchIndex / level)           // 横向滑动
                } else if (state == STATE_VERTICAL_DRAG) {
                    verticalDragEnd(touchIndex % level)         // 纵向滑动
                }
                touchIndex = -1             //初始化变量
                state = STATE_IDLE
            }

            else -> {}
        }
        return super.onTouchEvent(event)
    }

    /**
     * 判断点击的坐标是否再View的绘制范围内
     */
    private fun isPointInView(view: View?, x: Float, y: Float): Boolean {
        val location = IntArray(2)
        view!!.getLocationOnScreen(location)        //获取View的坐标
        val left = location[0]              //左坐标
        val top = location[1]               //上坐标
        val right = left + view.width
        val bottom = top + view.height
        return x >= left && x <= right && y >= top && y <= bottom       // 判断点击是否发生在View中
    }

    /**
     * 横向滑动中，实现循环滑动，处理backupDot出现的位置以及颜色
     */
    private fun horizontalDragging(rowIndex: Int, delta: Float) {
        val dots = ArrayList<ImageView>()           //获取一行的View
        for (i in 0 until level) {
            dots.add(dotViewArr[rowIndex * level + i])
        }
        val translationX = getValidTranslation(dots[0].translationX + delta)
        for (i in 0 until level) {
            dots[i].translationX = translationX         //对整行进行偏移移动
        }
        if (backupDot.visibility != View.VISIBLE) {
            backupDot.visibility = View.VISIBLE
        }
        if (translationX > 0) {
            // 向右滑，backup在左边出现
            backupDot.translationX = translationX - backupDot.width
            backupDot.setImageDrawable(dots[level - 1].drawable)         //右滑所以要将backup和划去的Dot一致
        } else {
            // 向左滑，backup在右边出现
            backupDot.translationX = backupDot.width * level + translationX
            backupDot.setImageDrawable(dots[0].drawable)
        }
        backupDot.translationY = (backupDot.height * rowIndex).toFloat()
    }

    /**
     * 横向滑动结束，判断滑动距离是否足够触发移动。如果触发移动后，刷新页面展示，并判断关卡是否通过
     */
    private fun horizontalDragEnd(rowIndex: Int) {
        val dots = ArrayList<ImageView>()
        for (i in 0 until level) {
            dots.add(dotViewArr[rowIndex * level + i])
        }
        val targetTranslationX = dots[0].translationX
        for (i in 0 until level) {
            dots[i].translationX = 0.0f
        }
        backupDot.visibility = View.INVISIBLE
        backupDot.translationX = 0.0f
        backupDot.translationY = 0.0f
        if (targetTranslationX > backupDot.width * 1.0f / 2) {
            // 最终向右移动一格，对数据进行交换
            if (level == 3) {
                val tmp = dotArr[rowIndex * level + 2]
                dotArr[rowIndex * level + 2] = dotArr[rowIndex * level + 1]
                dotArr[rowIndex * level + 1] = dotArr[rowIndex * level]
                dotArr[rowIndex * level] = tmp
            } else if (level == 4) {
                val tmp = dotArr[rowIndex * level + 3]
                dotArr[rowIndex * level + 3] = dotArr[rowIndex * level + 2]
                dotArr[rowIndex * level + 2] = dotArr[rowIndex * level + 1]
                dotArr[rowIndex * level + 1] = dotArr[rowIndex * level]
                dotArr[rowIndex * level] = tmp
            }

            fragment.refreshView()              //重新绘制视图
            judgeLevelPass()                    //判断是否过关
        } else if (targetTranslationX < backupDot.width * -1.0f / 2) {
            // 最终向左移动一格
            if (level == 3) {
                val tmp = dotArr[rowIndex * level]
                dotArr[rowIndex * level] = dotArr[rowIndex * level + 1]
                dotArr[rowIndex * level + 1] = dotArr[rowIndex * level + 2]
                dotArr[rowIndex * level + 2] = tmp
            } else if (level == 4) {
                val tmp = dotArr[rowIndex * level]
                dotArr[rowIndex * level] = dotArr[rowIndex * level + 1]
                dotArr[rowIndex * level + 1] = dotArr[rowIndex * level + 2]
                dotArr[rowIndex * level + 2] = dotArr[rowIndex * level + 3]
                dotArr[rowIndex * level + 3] = tmp
            }

            fragment.refreshView()
            judgeLevelPass()
        }
    }

    /**
     * 纵向滑动中，实现循环滑动
     */
    private fun verticalDragging(columnIndex: Int, delta: Float) {
        val dots = ArrayList<ImageView>()
        for (i in 0 until level) {
            dots.add(dotViewArr[columnIndex + level * i])
        }
        val translationY = getValidTranslation(dots[0].translationY + delta)
        for (i in 0 until level) {
            dots[i].translationY = translationY
        }
        if (backupDot.visibility != View.VISIBLE) {
            backupDot.visibility = View.VISIBLE
        }
        backupDot.translationX = (backupDot.width * columnIndex).toFloat()
        if (translationY > 0) {
            // 向下滑，backup在上边出现
            backupDot.translationY = translationY - backupDot.height
            backupDot.setImageDrawable(dots[level - 1].drawable)
        } else {
            // 向上滑，backup在下边出现
            backupDot.translationY = backupDot.height * level + translationY
            backupDot.setImageDrawable(dots[0].drawable)
        }
    }

    /**
     * 纵向滑动结束，判断滑动距离是否足够触发移动。如果触发移动后，刷新页面展示，并判断关卡是否通过
     */
    private fun verticalDragEnd(columnIndex: Int) {
        val dots = ArrayList<ImageView>()
        for (i in 0 until level) {
            dots.add(dotViewArr[columnIndex + level * i])
        }
        val targetTranslationY = dots[0].translationY
        for (i in 0 until level) {
            dots[i].translationY = 0.0f
        }
        backupDot.visibility = View.INVISIBLE
        backupDot.translationX = 0.0f
        backupDot.translationY = 0.0f
        if (targetTranslationY > backupDot.width * 1.0f / 2) {
            // 最终向下移动一格
            if (level == 3) {
                val tmp = dotArr[columnIndex + level * 2]
                dotArr[columnIndex + level * 2] = dotArr[columnIndex + level]
                dotArr[columnIndex + level] = dotArr[columnIndex]
                dotArr[columnIndex] = tmp
            } else if (level == 4) {
                val tmp = dotArr[columnIndex + level * 3]
                dotArr[columnIndex + level * 3] = dotArr[columnIndex + level * 2]
                dotArr[columnIndex + level * 2] = dotArr[columnIndex + level]
                dotArr[columnIndex + level] = dotArr[columnIndex]
                dotArr[columnIndex] = tmp
            }

            fragment.refreshView()
            judgeLevelPass()
        } else if (targetTranslationY < backupDot.width * -1.0f / 2) {
            // 最终向上移动一格
            if (level == 3) {
                val tmp = dotArr[columnIndex]
                dotArr[columnIndex] = dotArr[columnIndex + level]
                dotArr[columnIndex + level] = dotArr[columnIndex + level * 2]
                dotArr[columnIndex + level * 2] = tmp
            } else if (level == 4) {
                val tmp = dotArr[columnIndex]
                dotArr[columnIndex] = dotArr[columnIndex + level]
                dotArr[columnIndex + level] = dotArr[columnIndex + level * 2]
                dotArr[columnIndex + level * 2] = dotArr[columnIndex + level * 3]
                dotArr[columnIndex + level * 3] = tmp
            }
            fragment.refreshView()
            judgeLevelPass()
        }
    }

    /**
     * 限制一次只能滑动一格
     */
    private fun getValidTranslation(translation: Float): Float {
        return Math.max(
            (backupDot.width * -1).toFloat(),
            Math.min(translation, backupDot.width.toFloat())
        )
    }

    /**
     * 判断关卡是否通过，如果通过则使用SharedPreference记录完成记录和最佳成绩，重置布局
     */
    private fun judgeLevelPass() {
        for (i in 0 until level * level) {
            if (dotArr[i] != containerArr[i]) {
                return
            }
        }
        endTime = SystemClock.elapsedRealtime()     //获取结束时间
        binding.timer.stop()
        val totalTime = (endTime - startTime) / 1000.0      //计算总时间
        Toast.makeText(this, "恭喜过关, 用时${totalTime}秒", Toast.LENGTH_SHORT).show()

        var bestTime = sp.getString("record$level-$ringNum", "999")
        bestTime?.let {
            if (totalTime < it.toDouble()) {
                bestTime = totalTime.toString()
            }
        }
        val editor = sp.edit()
        editor.putBoolean("success$level-$ringNum", true)
        editor.putString("record$level-$ringNum", bestTime)
        editor.apply()

        initLevel(level, ringNum)
    }

}