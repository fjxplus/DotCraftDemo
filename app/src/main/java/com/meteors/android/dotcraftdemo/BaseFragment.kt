package com.meteors.android.dotcraftdemo

import android.widget.ImageView
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.collections.HashSet

private const val TAG = "BaseFragment"

/**
 * BaseFragment为FragmentLevel3、FragmentLevel4的父类
 * 包含游戏中的随机数据：白色DOT和白色RING的位置
 * 提供了更新视图、获取Dot和Ring引用的接口、需要子类进行实现
 */
abstract class BaseFragment(val level: Int): Fragment() {

    val containerArr = arrayOfNulls<Int>(level*level)
    val dotArr = arrayOfNulls<Int>(level*level)

    abstract fun refreshView()
    abstract fun getDotViewArray(): List<ImageView>
    abstract fun getContainerViewArray(): List<ImageView>
    abstract fun getBackupDot(): ImageView

    fun getDotArray(): Array<Int?> {
        return dotArr
    }
    fun getContainerArray(): Array<Int?> {
        return containerArr
    }

    /**
     * 初始化关卡
     * （可以在这里提高一下关卡难度，或者修改为多关卡）
     */
    fun initLevel(ringNum: Int) {

        val ringSet = HashSet<Int>()
        val dotSet = HashSet<Int>()
        for (i in 0 until ringNum) {
            var ringLocation: Int
            var dotLocation: Int
            do {
                ringLocation = (0 + Math.random() * ((level * level - 1) - 0 + 1)).toInt()
                dotLocation = (0 + Math.random() * ((level * level - 1) - 0 + 1)).toInt()

            } while (ringSet.contains(ringLocation) || dotSet.contains(dotLocation))
            ringSet.add(ringLocation)
            dotSet.add(dotLocation)
        }

        Arrays.fill(containerArr, 0)    //初始化Ring数组为0
        Arrays.fill(dotArr, 0)  //初始化Dot数组为0
        for (i in 0 until level * level) {
            if (ringSet.contains(i)) {
                containerArr[i] = 1
            }
            if (dotSet.contains(i)) {
                dotArr[i] = 1
            }
        }
        refreshView()               //刷新视图
    }

}