/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.databinding.BaseObservable
import android.databinding.Bindable
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.databinding.ObservableList
import android.graphics.drawable.Drawable
import com.example.changedetection.data.Diff

import com.example.changedetection.data.Task
import com.example.changedetection.data.source.DiffsDataSource
import com.example.changedetection.data.source.DiffsRepository
import com.example.changedetection.data.source.TasksDataSource
import com.example.changedetection.data.source.TasksRepository
import com.example.changedetection.data.source.local.SiteAndLastDiff
import com.example.changedetection.diffs.text.DiffRowGenerator
import com.example.changedetection.groupie.DiffItem
import com.example.changedetection.groupie.TextRecycler
import com.example.changedetection.util.SingleLiveEvent
import com.orhanobut.logger.Logger
import com.xwray.groupie.Section

import java.util.ArrayList


/**
 * Exposes the data to be used in the task list screen.
 *
 *
 * [BaseObservable] implements a listener registration mechanism which is notified when a
 * property changes. This is done by assigning a [Bindable] annotation to the property's
 * getter method.
 */
class TasksViewModel
//    private final SingleLiveEvent<Void> mNewTaskEvent = new SingleLiveEvent<>();

    (
    context: Application,
    //    private final SnackbarMessage mSnackbarText = new SnackbarMessage();

    //    private TasksFilterType mCurrentFiltering = TasksFilterType.ALL_TASKS;
    private val mDiffsRepository: DiffsRepository,
    private val mTasksRepository: TasksRepository
) : AndroidViewModel(context) {

    // These observable fields will update Views automatically
    val items: ObservableList<Task> = ObservableArrayList()

    val items2 = MutableLiveData<MutableList<SiteAndLastDiff>>()

    val dataLoading = ObservableBoolean(false)

    val currentFilteringLabel = ObservableField<String>()

    val noTasksLabel = ObservableField<String>()

    val noTaskIconRes = ObservableField<Drawable>()

    val empty = ObservableBoolean(false)

    val tasksAddViewVisible = ObservableBoolean()

    private val mTaskUpdated = SingleLiveEvent<Void>()

    private val mIsDataLoadingError = ObservableBoolean(false)

    //    private final SingleLiveEvent<String> mOpenTaskEvent = new SingleLiveEvent<>();

    private val mContext: Context // To avoid leaks, this must be an Application Context.

    init {
        mContext = context.applicationContext // Force use of Application Context.

        // Set initial state
        //        setFiltering(TasksFilterType.ALL_TASKS);
    }

    fun start() {
        loadTasks(false)
    }

    fun loadTasks(forceUpdate: Boolean) {
        loadTasks(forceUpdate, true)
    }

    //    /**
    //     * Sets the current task filtering type.
    //     *
    //     * @param requestType Can be {@link TasksFilterType#ALL_TASKS},
    //     *                    {@link TasksFilterType#COMPLETED_TASKS}, or
    //     *                    {@link TasksFilterType#ACTIVE_TASKS}
    //     */
    ////    public void setFiltering(TasksFilterType requestType) {
    //        mCurrentFiltering = requestType;
    //
    //        // Depending on the filter type, set the filtering label, icon drawables, etc.
    //        switch (requestType) {
    //            case ALL_TASKS:
    //                currentFilteringLabel.set(mContext.getString(R.string.label_all));
    //                noTasksLabel.set(mContext.getResources().getString(R.string.no_tasks_all));
    //                noTaskIconRes.set(mContext.getResources().getDrawable(
    //                        R.drawable.ic_assignment_turned_in_24dp));
    //                tasksAddViewVisible.set(true);
    //                break;
    //            case ACTIVE_TASKS:
    //                currentFilteringLabel.set(mContext.getString(R.string.label_active));
    //                noTasksLabel.set(mContext.getResources().getString(R.string.no_tasks_active));
    //                noTaskIconRes.set(mContext.getResources().getDrawable(
    //                        R.drawable.ic_check_circle_24dp));
    //                tasksAddViewVisible.set(false);
    //                break;
    //            case COMPLETED_TASKS:
    //                currentFilteringLabel.set(mContext.getString(R.string.label_completed));
    //                noTasksLabel.set(mContext.getResources().getString(R.string.no_tasks_completed));
    //                noTaskIconRes.set(mContext.getResources().getDrawable(
    //                        R.drawable.ic_verified_user_24dp));
    //                tasksAddViewVisible.set(false);
    //                break;
    //        }
    //    }

    fun clearCompletedTasks() {
        mTasksRepository.clearCompletedTasks()
        //        mSnackbarText.setValue(R.string.completed_tasks_cleared);
//        loadTasks(false, false)
    }

    fun completeTask(task: Task, completed: Boolean) {
        // Notify repository
        if (completed) {
            mTasksRepository.completeTask(task)
            //            showSnackbarMessage(R.string.task_marked_complete);
        } else {
            mTasksRepository.activateTask(task)
            //            showSnackbarMessage(R.string.task_marked_active);
        }
    }

    // Called when clicking on fab.
    internal fun saveTask(title: String, url: String, timestamp: Long): Task {
        var task = Task(title, url, timestamp)
//        if (task.isEmpty()) {
//            mSnackbarText.setValue(R.string.empty_task_message)
//            return
//        }
//        if (isNewTask() || mTaskId == null) {
//            createTask(task)
//        } else {
//            task = Task(title.get(), url.get(), mTaskId, mTaskCompleted)
//            updateTask(task)
//        }

        mTasksRepository.saveTask(task)
        mTaskUpdated.call()

        return task
    }

    // Called when clicking on fab.
    internal fun saveWebsite(diff: Diff): MutableLiveData<Boolean> {
        val didItWork = MutableLiveData<Boolean>()

        mDiffsRepository.saveDiff(diff, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                didItWork.value = true
            }

            override fun onDataNotAvailable() {
                didItWork.value = false
            }
        })
        mTaskUpdated.call()
        return didItWork
    }

    internal fun updateTask(task: Task) {
        mTasksRepository.saveTask(task)
        mTaskUpdated.call()
    }

    internal fun getWebHistoryForId(id: String): MutableLiveData<List<Diff>> {
        val diffItems = MutableLiveData<List<Diff>>()

        mDiffsRepository.getDiffs(id, object : DiffsDataSource.LoadDiffsCallback {
            override fun onDiffsLoaded(diffs: List<Diff>) {
                diffItems.value = diffs.toMutableList().sortedByDescending { it.timestamp }
            }

            override fun onDataNotAvailable() {
                mIsDataLoadingError.set(true)
            }
        })

        return diffItems
    }

    //    SnackbarMessage getSnackbarMessage() {
    //        return mSnackbarText;
    //    }
    //
    //    SingleLiveEvent<String> getOpenTaskEvent() {
    //        return mOpenTaskEvent;
    //    }
    //
    //    SingleLiveEvent<Void> getNewTaskEvent() {
    //        return mNewTaskEvent;
    //    }

    //    private void showSnackbarMessage(Integer message) {
    //        mSnackbarText.setValue(message);
    //    }

    //    /**
    //     * Called by the Data Binding library and the FAB's click listener.
    //     */
    //    public void addNewTask() {
    //        mNewTaskEvent.call();
    //    }
    //
    //    void handleActivityResult(int requestCode, int resultCode) {
    //        if (AddEditTaskActivity.REQUEST_CODE == requestCode) {
    //            switch (resultCode) {
    //                case TaskDetailActivity.EDIT_RESULT_OK:
    //                    mSnackbarText.setValue(R.string.successfully_saved_task_message);
    //                    break;
    //                case AddEditTaskActivity.ADD_EDIT_RESULT_OK:
    //                    mSnackbarText.setValue(R.string.successfully_added_task_message);
    //                    break;
    //                case TaskDetailActivity.DELETE_RESULT_OK:
    //                    mSnackbarText.setValue(R.string.successfully_deleted_task_message);
    //                    break;
    //            }
    //        }
    //    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the [TasksDataSource]
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private fun loadTasks(forceUpdate: Boolean, showLoadingUI: Boolean) {
        if (showLoadingUI) {
            dataLoading.set(true)
        }
        if (forceUpdate) {
            mTasksRepository.refreshTasks()
        }

        mTasksRepository.getTaskAndDiffs {
            // We filter the tasks based on the requestType
            if (showLoadingUI) {
                dataLoading.set(false)
            }
            mIsDataLoadingError.set(false)

            items2.value = it
            empty.set(items.isEmpty())
        }
    }



    private fun diffagain(section: Section, original: Diff?, new: Diff?) {
        val (onlyDiff, nonDiff) = generateDiffRows(original, new)

        updatingOnlyDiff.clear()
        updatingOnlyDiff.addAll(onlyDiff)

        updatingNonDiff.clear()
        updatingNonDiff.addAll(nonDiff)

        updateSection(section)
    }

    fun onClick(item: DiffItem?, updatinghor: MutableList<DiffItem>, section: Section){
        // This is a simple Finite State Machine
        if (item !is DiffItem) {
            return
        }

        when (item.colorSelected) {
            2 -> {
                // ORANGE -> GREY
                item.notifyChanged(0)
            }
            1 -> {
                // AMBER -> GREY
                item.notifyChanged(0)
            }
            else -> {
                when (updatinghor.count { it.colorSelected > 0 }) {
                    0 -> {
                        // NOTHING IS SELECTED -> AMBER
                        item.notifyChanged(1)
                    }
                    1 -> {
                        // ONE THING IS SELECTED AND IT IS AMBER -> ORANGE
                        // ONE THING IS SELECTED AND IT IS ORANGE -> AMBER
                        when (updatinghor.firstOrNull { it.colorSelected > 0 }?.colorSelected) {
                            2 -> {
                                item.notifyChanged(1)
                                diffagain(
                                    section,
                                    item.diff,
                                    updatinghor.first { it.colorSelected == 2 }.diff
                                )
                            }
                            else -> {
                                item.notifyChanged(2)
                                diffagain(
                                    section,
                                    updatinghor.first { it.colorSelected == 1 }.diff,
                                    item.diff
                                )
                            }
                        }
                    }
                    else -> {
                        // TWO ARE SELECTED. UNSELECT THE ORANGE, SELECT ANOTHER THING.
                        updatinghor.first { it.colorSelected >= 2 }.notifyChanged(0)
                        diffagain(
                            section,
                            updatinghor.first { it.colorSelected == 1 }.diff,
                            item.diff
                        )
                        item.notifyChanged(2)
                    }
                }
            }
        }
    }

    fun generateDiffRows(
        original: Diff?,
        it: Diff?
    ): Pair<MutableList<TextRecycler>, MutableList<TextRecycler>> {
        if (original == null || it == null) {
            Logger.d("original or it are null")
            return Pair(mutableListOf(), mutableListOf())
        }

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { f -> "TEXTREMOVED" }
            .newTag { f -> "TEXTADDED" }
            .build()

        //compute the differences for two test texts.
        val rows = generator.generateDiffRows(
            it.value.split("\n"),
            original.value.split("\n")
        )

        val updatingNonDiff = mutableListOf<TextRecycler>()
        val updatingOnlyDiff = mutableListOf<TextRecycler>()

        rows.forEachIndexed { index, row ->
            if (row.oldLine == row.newLine) {
                updatingNonDiff.add(TextRecycler(row.oldLine, index))
                println("$index none: " + row.oldLine)
            } else {
                when {
                    row.newLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        println("$index old: " + row.oldLine)
                    }
                    row.oldLine.isBlank() -> {
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))
                        println("$index new: " + row.newLine)
                    }
                    else -> {
                        updatingOnlyDiff.add(TextRecycler("-" + row.oldLine, index))
                        updatingOnlyDiff.add(TextRecycler("+" + row.newLine, index))

                        println("$index old: " + row.oldLine)
                        println("$index new: " + row.newLine)
                    }
                }
            }
        }

        return Pair(updatingOnlyDiff, updatingNonDiff)
    }
}
