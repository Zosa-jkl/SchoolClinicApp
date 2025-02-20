package com.example.schoolclinicappointment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AppointmentsPagerAdapter extends FragmentStateAdapter {

    public AppointmentsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new PhysicalExamFragment();
        } else {
            return new DentalExamFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs
    }
}
