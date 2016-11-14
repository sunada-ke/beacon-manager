package com.altla.vision.beacon.manager.presentation.view;

import com.altla.vision.beacon.manager.presentation.presenter.ProjectSwitchPresenter;
import com.altla.vision.beacon.manager.presentation.presenter.model.NameSpaceModel;

public interface ProjectSwitchItemView extends ItemView {

    void setItemPresenter(ProjectSwitchPresenter.ProjectSwitchItemPresenter itemPresenter);

    void showItem(NameSpaceModel model);
}