package com.spotlight.spotlightapp

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commit
import com.spotlight.spotlightapp.data.task.Task
import com.spotlight.spotlightapp.splash.SplashScreenFragment
import com.spotlight.spotlightapp.task.TaskPageRouter
import com.spotlight.spotlightapp.task.views.CurrentTaskFragment
import com.spotlight.spotlightapp.task.views.DailyIntentListFragment
import com.spotlight.spotlightapp.task.views.TaskFormFragment
import com.spotlight.spotlightapp.task.views.TasksFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main), TaskPageRouter {
    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    private val fragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (loadFragmentClass(classLoader, className)) {
                DailyIntentListFragment::class.java -> DailyIntentListFragment(this@MainActivity)
                CurrentTaskFragment::class.java -> CurrentTaskFragment(this@MainActivity).apply {
                    addSharedElementTransition()
                }
                TasksFragment::class.java -> TasksFragment(this@MainActivity).apply {
                    addSharedElementTransition()
                }
                TaskFormFragment::class.java -> TaskFormFragment().apply {
                    addSharedElementTransition()
                }
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)

        configureSplashScreenFragment()
        observeViewModel()
    }

    override fun openTaskList(view: View) {
        attachFragment(
            TasksFragment::class.java, TasksFragment.TAG, willAddToBackStack = true,
            sharedElements = mapOf(Pair(view, view.transitionName)))
    }

    override fun openTaskPage(view: View, task: Task, willAllowEdit: Boolean) {
        val bundle = Bundle().apply {
            putParcelable(CurrentTaskFragment.TASK, task)
            putBoolean(CurrentTaskFragment.WILL_ALLOW_EDIT, willAllowEdit)
        }

        attachFragment(
            CurrentTaskFragment::class.java, CurrentTaskFragment.TAG,
            willAddToBackStack = true, arguments = bundle,
            sharedElements = mapOf(Pair(view, view.transitionName)))
    }


    override fun openTaskForm(view: View, task: Task?) {
        val bundle = task?.let {
            Bundle().apply { putParcelable(TaskFormFragment.TASK, it) }
        }

        attachFragment(
            TaskFormFragment::class.java, TaskFormFragment.TAG,
            willAddToBackStack = true, arguments = bundle,
            sharedElements = mapOf(Pair(view, view.transitionName)))
    }

    private fun configureSplashScreenFragment() {
        supportFragmentManager.apply {
            setFragmentResultListener(
                SplashScreenFragment.REQUEST_KEY,
                this@MainActivity) { requestKey, result ->
                if (requestKey == SplashScreenFragment.REQUEST_KEY &&
                    result.getBoolean(SplashScreenFragment.IS_FINISHED_SPLASH_ANIMATION, false)) {
                    configureDailyIntentListFragment()
                    mainActivityViewModel.checkForRunningTask()

                }
            }

            attachFragment(SplashScreenFragment::class.java, SplashScreenFragment.TAG)
        }
    }

    private fun observeViewModel() {
        mainActivityViewModel.currentRunningTask.observe(this) { currentRunningTask ->
            if (currentRunningTask == null) return@observe

            val bundle = Bundle().apply {
                putParcelable(CurrentTaskFragment.TASK, currentRunningTask)
                putBoolean(CurrentTaskFragment.WILL_ALLOW_EDIT, false)
            }
            attachFragment(
                CurrentTaskFragment::class.java, CurrentTaskFragment.TAG, willAddToBackStack = true,
                arguments = bundle)
            mainActivityViewModel.notifyCurrentRunningTaskHandled()
        }
    }

    private fun configureDailyIntentListFragment() {
        attachFragment(DailyIntentListFragment::class.java, DailyIntentListFragment.TAG)
    }

    private fun attachFragment(
        fragmentClass: Class<out Fragment>, tag: String, willAddToBackStack: Boolean = false,
        arguments: Bundle? = null, sharedElements: Map<View, String> = mapOf()) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            sharedElements.forEach {
                addSharedElement(it.key, it.value)
            }
            replace(R.id.fragmentContainer, fragmentClass, arguments, tag)

            if (willAddToBackStack) {
                addToBackStack(tag)
            }
        }
    }

    private fun Fragment.addSharedElementTransition() {
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeTransform())
            .addTransition(ChangeBounds())

        sharedElementEnterTransition = sharedElementTransition
        returnTransition = Fade()
        postponeEnterTransition()
    }
}
