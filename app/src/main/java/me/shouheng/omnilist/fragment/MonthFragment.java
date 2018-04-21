package me.shouheng.omnilist.fragment;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.jeek.calendar.widget.calendar.tools.OnCalendarClickListener;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import me.shouheng.omnilist.R;
import me.shouheng.omnilist.activity.ContentActivity;
import me.shouheng.omnilist.adapter.AssignmentsAdapter;
import me.shouheng.omnilist.config.Constants;
import me.shouheng.omnilist.databinding.FragmentMonthCalendarBinding;
import me.shouheng.omnilist.databinding.ItemTitleBinding;
import me.shouheng.omnilist.fragment.base.BaseFragment;
import me.shouheng.omnilist.model.Assignment;
import me.shouheng.omnilist.utils.TimeUtils;
import me.shouheng.omnilist.utils.ToastUtils;
import me.shouheng.omnilist.viewmodel.AssignmentViewModel;
import me.shouheng.omnilist.widget.tools.CustomItemAnimator;
import me.shouheng.omnilist.widget.tools.DividerItemDecoration;

public class MonthFragment extends BaseFragment<FragmentMonthCalendarBinding> implements OnCalendarClickListener {

    private final static int REQUEST_EDIT = 0x0FF1;

    private AssignmentViewModel assignmentViewModel;

    private RecyclerView.OnScrollListener onScrollListener;

    private AssignmentsAdapter mAdapter;

    private boolean isContentChanged = false;

    private ItemTitleBinding itemTitleBinding;

    public static MonthFragment newInstance() {
        Bundle args = new Bundle();
        MonthFragment fragment = new MonthFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_month_calendar;
    }

    @Override
    protected void doCreateView(Bundle savedInstanceState) {
        configToolbar();

        configViewModel();

        configCalendarTheme();

        configEvents();

        configList();

        initValues();
    }

    private void configToolbar() {
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.drawer_menu_calendar);
            }
        }
    }

    private void configCalendarTheme() {
        if (isDarkTheme()) {
            getBinding().weekBar.setBackgroundResource(R.color.dark_theme_background);

            getBinding().mcvCalendar.setNormalDayColor(Color.WHITE);
            getBinding().mcvCalendar.setBackgroundResource(R.color.dark_theme_background);

            getBinding().wcvCalendar.setNormalDayColor(Color.WHITE);
            getBinding().wcvCalendar.setBackgroundResource(R.color.dark_theme_background);

            getBinding().rlScheduleList.setBackgroundResource(R.color.dark_theme_background);
            getBinding().rvScheduleList.setBackgroundResource(R.color.dark_theme_background);
            getBinding().rlNoTask.setBackgroundResource(R.color.dark_theme_background);
        }

        getBinding().wcvCalendar.setSelectedBGColor(primaryColor());
        getBinding().wcvCalendar.setSelectBGTodayColor(primaryColor());
        getBinding().wcvCalendar.setCurrentDayColor(primaryColor());
        getBinding().wcvCalendar.setHolidayTextColor(primaryColor());

        getBinding().mcvCalendar.setSelectedBGColor(primaryColor());
        getBinding().mcvCalendar.setSelectBGTodayColor(primaryColor());
        getBinding().mcvCalendar.setCurrentDayColor(primaryColor());
        getBinding().mcvCalendar.setHolidayTextColor(primaryColor());
    }

    private void configEvents() {
        getBinding().wcvCalendar.addOnCalendarClickListener(this);
        getBinding().mcvCalendar.addOnCalendarClickListener(this);
    }

    private void configList() {
        itemTitleBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.item_title, null, false);

        mAdapter = new AssignmentsAdapter(Collections.emptyList());
        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            switch (view.getId()) {
                case R.id.iv_completed:
                    Assignment assignment = mAdapter.getItem(position);
                    assert assignment != null;
                    if (assignment.getProgress() == Constants.MAX_ASSIGNMENT_PROGRESS) {
                        assignment.setProgress(0);
                        assignment.setInCompletedThisTime(true);
                    } else {
                        assignment.setProgress(Constants.MAX_ASSIGNMENT_PROGRESS);
                        assignment.setCompleteThisTime(true);
                    }
                    assignment.setChanged(!assignment.isChanged());
                    mAdapter.setStateChanged(true);
                    mAdapter.notifyItemChanged(position);
                    updateState();
                    break;
                case R.id.rl_item:
                    ContentActivity.editAssignment(MonthFragment.this,
                            Objects.requireNonNull(mAdapter.getItem(position)), REQUEST_EDIT);
                    break;
            }
        });
        mAdapter.addHeaderView(itemTitleBinding.getRoot());

        getBinding().rvScheduleList.setLayoutManager(new LinearLayoutManager(getContext()));
        getBinding().rvScheduleList.setHasFixedSize(true);
        getBinding().rvScheduleList.addItemDecoration(new DividerItemDecoration(
                Objects.requireNonNull(getContext()), DividerItemDecoration.VERTICAL_LIST, isDarkTheme()));
        getBinding().rvScheduleList.setItemAnimator(new CustomItemAnimator());
        getBinding().rvScheduleList.setLayoutManager(new LinearLayoutManager(getActivity()));
        getBinding().rvScheduleList.setAdapter(mAdapter);
        getBinding().rvScheduleList.setEmptyView(getBinding().rlNoTask);

        if (onScrollListener != null) {
            getBinding().rvScheduleList.addOnScrollListener(onScrollListener);
        }
    }

    private void configViewModel() {
        assignmentViewModel = ViewModelProviders.of(this).get(AssignmentViewModel.class);
    }

    private void initValues() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        /*Use current date as clicked date.*/
        showDateSubTitle(year, month, day);

        /*Load assignments today.*/
        loadAssignment(year, month, day);
    }

    private void showDateSubTitle(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(TimeUtils.getNoMonthDay(getContext(), calendar.getTime()));
            }
        }

        itemTitleBinding.tvSectionTitle.setText(TimeUtils.getShortDate(getContext(), calendar));
    }

    @Override
    public void onClickDate(int year, int month, int day) {
        showDateSubTitle(year, month, day);
        loadAssignment(year, month, day);
    }

    private void updateState() {
        assignmentViewModel.updateAssignments(mAdapter.getData()).observe(this, listResource -> {
            if (listResource == null) {
                ToastUtils.makeToast(R.string.text_error_when_save);
                return;
            }
            switch (listResource.status) {
                case FAILED:
                    ToastUtils.makeToast(R.string.text_error_when_save);
                    break;
                case SUCCESS:
                    ToastUtils.makeToast(R.string.text_update_successfully);
                    isContentChanged = true;
                    break;
            }
        });
    }

    private void loadAssignment(int year, int month, int day) {
        DateTime dateTime = new DateTime(year, month + 1, day, 0, 0);
        Date startDate = TimeUtils.startTime(dateTime);
        Date endDate = TimeUtils.endTime(dateTime);
        assignmentViewModel.getAssignments(startDate.getTime(), endDate.getTime()).observe(this, listResource -> {
            assert listResource != null;
            switch (listResource.status) {
                case FAILED:
                    ToastUtils.makeToast(R.string.text_failed_to_load_data);
                    break;
                case SUCCESS:
                    assert listResource.data != null;
                    List<Assignment> assignments = filterAssignments(listResource.data, year, month, day);
                    sortAssignments(assignments);
                    mAdapter.setNewData(assignments);
                    break;
            }
        });
    }

    private List<Assignment> filterAssignments(List<Assignment> assignments, int year, int month, int day) {
        int week = new DateTime(year, month + 1, day, 0, 0).getDayOfWeek();
        List<Assignment> ret = new LinkedList<>();
        /*Will return if the assignment is one shot or the repeat week contains today.*/
        for (Assignment assignment : assignments) {
            if (assignment.getDaysOfWeek().getCoded() == 0
                    || (1 << (week - 1) & assignment.getDaysOfWeek().getCoded()) != 0) {
                ret.add(assignment);
            }
        }
        return ret;
    }

    private void sortAssignments(List<Assignment> assignments) {
        Collections.sort(assignments, (o1, o2) -> o1.getNoticeTime() - o2.getNoticeTime());
    }

    public void setOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT:
                    isContentChanged = true;
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
