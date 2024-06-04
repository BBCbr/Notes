package com.example.notes

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun addButton_isDisplayed() {
        // Проверка, что кнопка добавления отображается
        onView(withId(R.id.add_n)).check(matches(isDisplayed()))
    }

    @Test
    fun addNoteButton_worksCorrectly() {
        // Нажатие на кнопку добавления заметки и проверка диалога
        onView(withId(R.id.add_n)).perform(click())
        onView(withText("Add Note")).check(matches(isDisplayed()))

        // Ввод текста заметки
        onView(isAssignableFrom(EditText::class.java)).perform(typeText("Test Note"), pressImeActionButton())
        onView(withText("OK")).perform(click())

        Thread.sleep(1000)

        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.scrollToPosition<Adapter1.ViewHolder>(0))
            .check(matches(hasDescendant(withText("Test Note"))))
    }

    @Test
    fun noteList_isDisplayed() {
        onView(withId(R.id.recycle)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteNoteButton_worksCorrectly() {
        onView(withId(R.id.add_n)).perform(click())
        onView(withText("Add Note")).check(matches(isDisplayed()))
        // Ввод текста заметки
        onView(isAssignableFrom(EditText::class.java)).perform(typeText("Test Note"), pressImeActionButton())
        onView(withText("OK")).perform(click())

        Thread.sleep(1000)

        // Проверка, что заметка добавлена в RecyclerView
        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.scrollToPosition<Adapter1.ViewHolder>(0))
            .check(matches(hasDescendant(withText("Test Note"))))

        // Нажатие на кнопку удаления заметки
        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.actionOnItemAtPosition<Adapter1.ViewHolder>(0, MyViewAction.clickChildViewWithId(R.id.but_del)))
    }

    @Test
    fun searchFunctionality_worksCorrectly() {
        onView(withId(R.id.add_n)).perform(click())
        onView(withText("Add Note")).check(matches(isDisplayed()))

        // Ввод текста заметки
        onView(isAssignableFrom(android.widget.EditText::class.java)).perform(typeText("Test Note1"), pressImeActionButton())
        onView(withText("OK")).perform(click())

        Thread.sleep(1000)

        onView(withId(R.id.search_view)).perform(clickInLeft())

        onView(isAssignableFrom(EditText::class.java)).perform(typeText("Test Note1"))

        onView(isRoot()).perform(closeSoftKeyboard())

        onView(withId(R.id.note_short)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(R.id.note_short)).check(matches(withText(containsString("Test Note1"))))
    }

    @Test
    fun editNoteButton_worksCorrectly() {
        onView(withId(R.id.add_n)).perform(click())
        onView(withText("Add Note")).check(matches(isDisplayed()))

        onView(isAssignableFrom(EditText::class.java)).perform(typeText("Test Note"), pressImeActionButton())
        onView(withText("OK")).perform(click())

        Thread.sleep(1000)

        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.scrollToPosition<Adapter1.ViewHolder>(0))
            .check(matches(hasDescendant(withText("Test Note"))))

        // Нажатие на кнопку редактирования заметки
        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.actionOnItemAtPosition<Adapter1.ViewHolder>(0, MyViewAction.clickChildViewWithId(R.id.but_edit)))

        // Проверка открытия диалога редактирования
        onView(withText("Edit Note")).check(matches(isDisplayed()))
    }

    @Test
    fun selectNoteButton_worksCorrectly() {
        onView(withId(R.id.add_n)).perform(click())
        onView(withText("Add Note")).check(matches(isDisplayed()))

        onView(isAssignableFrom(EditText::class.java)).perform(typeText("Test Note"), pressImeActionButton())
        onView(withText("OK")).perform(click())

        Thread.sleep(1000)

        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.scrollToPosition<Adapter1.ViewHolder>(0))
            .check(matches(hasDescendant(withText("Test Note"))))

        onView(withId(R.id.recycle))
            .perform(RecyclerViewActions.actionOnItemAtPosition<Adapter1.ViewHolder>(0, MyViewAction.clickChildViewWithId(R.id.but_select)))

    }
}

// Вспомогательное действие для клика по левой части `SearchView`
private fun clickInLeft(): ViewAction {
    return GeneralClickAction(
        Tap.SINGLE,
        { view ->
            val screenPos = IntArray(2)
            view.getLocationOnScreen(screenPos)
            val x = screenPos[0] + view.width / 8f // Левая сторона `SearchView`
            val y = screenPos[1] + view.height / 2f // Вертикально в середине
            floatArrayOf(x, y)
        },
        Press.FINGER,
        InputDevice.SOURCE_TOUCHSCREEN,
        MotionEvent.BUTTON_PRIMARY
    )
}

// Вспомогательный класс для выполнения действия на дочернем представлении в RecyclerView
object MyViewAction {
    fun clickChildViewWithId(id: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(View::class.java)
        }

        override fun getDescription(): String {
            return "Click on a child view with specified id."
        }

        override fun perform(uiController: UiController?, view: View) {
            val v = view.findViewById<View>(id)
            v.performClick()
        }
    }
}