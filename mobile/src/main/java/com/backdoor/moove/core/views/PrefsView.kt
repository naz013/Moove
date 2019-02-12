package com.backdoor.moove.core.views

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.DrawableRes
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView

import com.backdoor.moove.R

/**
 * Copyright 2015 Nazar Suhovich
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class PrefsView : RelativeLayout {

    private val check = 0
    private val text = 2
    private val view = 1
    private val none = 3

    private var checkBox: CheckBox? = null
    private var title: TextView? = null
    private var detail: TextView? = null
    private var prefsValue: TextView? = null
    private var dividerTop: View? = null
    private var dividerBottom: View? = null
    private var prefsView: View? = null

    var isChecked: Boolean = false
        set(checked) {
            field = checked
            checkBox!!.isChecked = checked
        }
    private var viewType = check

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.prefs_view_layout, this)
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        title = findViewById(R.id.prefsPrimaryText)
        detail = findViewById(R.id.prefsSecondaryText)
        prefsValue = findViewById(R.id.prefsValue)
        checkBox = findViewById(R.id.prefsCheck)

        dividerTop = findViewById(R.id.dividerTop)
        dividerBottom = findViewById(R.id.dividerBottom)
        prefsView = findViewById(R.id.prefsView)

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                    attrs, R.styleable.PrefsView, 0, 0)

            var titleText: String? = ""
            var detailText: String? = ""
            var valueText: String? = ""
            var divTop = false
            var divBottom = false
            var res = 0

            try {
                titleText = a.getString(R.styleable.PrefsView_prefs_primary_text)
                detailText = a.getString(R.styleable.PrefsView_prefs_secondary_text)
                valueText = a.getString(R.styleable.PrefsView_prefs_value_text)
                divTop = a.getBoolean(R.styleable.PrefsView_prefs_divider_top, false)
                divBottom = a.getBoolean(R.styleable.PrefsView_prefs_divider_bottom, false)
                viewType = a.getInt(R.styleable.PrefsView_prefs_type, check)
                res = a.getInt(R.styleable.PrefsView_prefs_view_resource, 0)
            } catch (e: Exception) {
                Log.e("PrefsView", "There was an error loading attributes.")
            } finally {
                a.recycle()
            }

            setTitleText(titleText)
            setDetailText(detailText)
            setDividerTop(divTop)
            setDividerBottom(divBottom)
            setView()
            setValueText(valueText)
            setViewResource(res)
        }
        isChecked = isChecked
    }

    private fun setView() {
        if (viewType == check) {
            checkBox!!.visibility = View.VISIBLE
            prefsValue!!.visibility = View.GONE
            prefsView!!.visibility = View.GONE
        } else if (viewType == text) {
            checkBox!!.visibility = View.GONE
            prefsValue!!.visibility = View.VISIBLE
            prefsView!!.visibility = View.GONE
        } else if (viewType == view) {
            checkBox!!.visibility = View.GONE
            prefsValue!!.visibility = View.GONE
            prefsView!!.visibility = View.VISIBLE
        } else {
            checkBox!!.visibility = View.GONE
            prefsValue!!.visibility = View.GONE
            prefsView!!.visibility = View.GONE
        }
    }

    fun setTitleText(text: String) {
        title!!.text = text
    }

    fun setDetailText(text: String?) {
        if (text == null) {
            detail!!.visibility = View.GONE
            return
        }
        detail!!.text = text
        detail!!.visibility = View.VISIBLE
    }

    fun setValueText(text: String) {
        prefsValue!!.text = text
    }

    fun setViewResource(@DrawableRes resource: Int) {
        if (resource != 0) {
            prefsView!!.setBackgroundResource(resource)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        checkBox!!.isEnabled = enabled
        prefsView!!.isEnabled = enabled
        prefsValue!!.isEnabled = enabled
        detail!!.isEnabled = enabled
        title!!.isEnabled = enabled
    }

    fun setDividerTop(divider: Boolean) {
        if (divider) {
            dividerTop!!.visibility = View.VISIBLE
        } else {
            dividerTop!!.visibility = View.GONE
        }
    }

    fun setDividerBottom(divider: Boolean) {
        if (divider) {
            dividerBottom!!.visibility = View.VISIBLE
        } else {
            dividerBottom!!.visibility = View.GONE
        }
    }
}
