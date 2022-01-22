package com.meteors.android.dotcraftdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.meteors.android.dotcraftdemo.databinding.FragmentLevel4Binding

/**
 * 4x4的Fragment视图，实现获取棋盘中View的引用，更新布局的方法
 */
class FragmentLevel4: BaseFragment(4) {
    private var _binding: FragmentLevel4Binding? =null
    private val binding get() = _binding!!

    private lateinit var containerViewArr: List<ImageView>
    private lateinit var dotViewArr: List<ImageView>

    private lateinit var backupDot: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevel4Binding.inflate(inflater, container, false)
        containerViewArr = listOf(
            binding.container0,
            binding.container1,
            binding.container2,
            binding.container3,
            binding.container4,
            binding.container5,
            binding.container6,
            binding.container7,
            binding.container8,
            binding.container9,
            binding.container10,
            binding.container11,
            binding.container12,
            binding.container13,
            binding.container14,
            binding.container15
        )
        dotViewArr = listOf(
            binding.dot0,
            binding.dot1,
            binding.dot2,
            binding.dot3,
            binding.dot4,
            binding.dot5,
            binding.dot6,
            binding.dot7,
            binding.dot8,
            binding.dot9,
            binding.dot10,
            binding.dot11,
            binding.dot12,
            binding.dot13,
            binding.dot14,
            binding.dot15
        )
        backupDot = binding.backupDot
        return binding.root
    }

    override fun refreshView() {
        for (i in 0 until level * level) {
            if (containerArr[i] == 1) {         //指定位置加载shape_ring_white
                containerViewArr[i].setImageResource(R.drawable.shape_ring_white)
            } else {
                containerViewArr[i].setImageResource(0)
            }
        }
        for (i in 0 until level * level) {
            if (dotArr[i] == 1) {               //加载黑白Dot
                dotViewArr[i].setImageResource(R.drawable.shape_dot_white)
            } else {
                dotViewArr[i].setImageResource(R.drawable.shape_dot_black)
            }
        }
    }

    override fun getContainerViewArray(): List<ImageView> {
        return containerViewArr
    }

    override fun getBackupDot(): ImageView {
        return backupDot
    }

    override fun getDotViewArray(): List<ImageView> {
        return dotViewArr
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}