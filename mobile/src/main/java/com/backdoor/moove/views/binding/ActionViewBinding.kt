package com.backdoor.moove.views.binding

import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import com.backdoor.moove.R
import com.backdoor.moove.views.PhoneAutoCompleteView
import com.google.android.material.textfield.TextInputLayout

class ActionViewBinding(view: View) : Binding(view) {
    val actionCheck: CheckBox by bindView(R.id.actionCheck)
    val actionBlock: View by bindView(R.id.actionBlock)
    val radioGroup: RadioGroup by bindView(R.id.radioGroup)
    val callAction: RadioButton by bindView(R.id.callAction)
    val messageAction: RadioButton by bindView(R.id.messageAction)
    val numberLayout: TextInputLayout by bindView(R.id.numberLayout)
    val numberView: PhoneAutoCompleteView by bindView(R.id.numberView)
    val selectNumber: View by bindView(R.id.selectNumber)
}