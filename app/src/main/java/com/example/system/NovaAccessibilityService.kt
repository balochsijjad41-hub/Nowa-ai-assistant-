package com.example.system

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NovaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events processed on demand when user issues screen voice commands
    }

    override fun onInterrupt() {
        _isConnected.value = false
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
        super.onDestroy()
    }

    fun performSystemBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performSystemHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performSystemRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun performSystemScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun clickNodeByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matches = root.findAccessibilityNodeInfosByText(targetText)
        for (node in matches) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                current = current.parent
            }
        }
        return false
    }

    fun collectScreenLabels(limit: Int = 12): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val labels = mutableListOf<String>()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || labels.size >= limit) return
            val label = node.text?.toString()?.trim()
                ?: node.contentDescription?.toString()?.trim()
            if (!label.isNullOrBlank() && label.length <= 60) {
                labels.add(label)
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }
        traverse(root)
        return labels.distinct()
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        @Volatile
        var instance: NovaAccessibilityService? = null
            private set
    }
}
