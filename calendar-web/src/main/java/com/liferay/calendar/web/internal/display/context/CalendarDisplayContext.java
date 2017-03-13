/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.calendar.web.internal.display.context;

import com.liferay.calendar.constants.CalendarActionKeys;
import com.liferay.calendar.model.Calendar;
import com.liferay.calendar.model.CalendarResource;
import com.liferay.calendar.service.CalendarLocalService;
import com.liferay.calendar.service.CalendarService;
import com.liferay.calendar.service.permission.CalendarPermission;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Adam Brandizzi
 */
public class CalendarDisplayContext {

	public CalendarDisplayContext(
		GroupLocalService groupLocalService, CalendarService calendarService,
		CalendarLocalService calendarLocalService, ThemeDisplay themeDisplay) {

		_groupLocalService = groupLocalService;
		_calendarService = calendarService;
		_calendarLocalService = calendarLocalService;
		_themeDisplay = themeDisplay;
	}

	public Calendar getDefaultCalendar(
		List<Calendar> groupCalendars, List<Calendar> userCalendars) {

		Calendar defaultCalendar = null;

		for (Calendar groupCalendar : groupCalendars) {
			if (groupCalendar.isDefaultCalendar() &&
				CalendarPermission.contains(
					_themeDisplay.getPermissionChecker(), groupCalendar,
					CalendarActionKeys.MANAGE_BOOKINGS)) {

				defaultCalendar = groupCalendar;
			}
		}

		if (defaultCalendar == null) {
			for (Calendar userCalendar : userCalendars) {
				if (userCalendar.isDefaultCalendar()) {
					defaultCalendar = userCalendar;
				}
			}
		}

		if (defaultCalendar == null && !groupCalendars.isEmpty()) {
			for (Calendar groupCalendar : groupCalendars) {
				if (groupCalendar.isDefaultCalendar() &&
					CalendarPermission.contains(
						_themeDisplay.getPermissionChecker(), groupCalendar,
						CalendarActionKeys.VIEW_BOOKING_DETAILS)) {

					defaultCalendar = groupCalendar;
				}
			}
		}

		return defaultCalendar;
	}

	public List<Calendar> getOtherCalendars(long[] calendarIds)
		throws PortalException {

		List<Calendar> otherCalendars = new ArrayList<>();

		for (long calendarId : calendarIds) {
			Calendar calendar = _calendarService.fetchCalendar(calendarId);

			if (calendar == null) {
				continue;
			}

			CalendarResource calendarResource = calendar.getCalendarResource();

			if (!calendarResource.isActive()) {
				continue;
			}

			Group scopeGroup = _themeDisplay.getScopeGroup();

			long scopeGroupId = scopeGroup.getGroupId();
			long scopeLiveGroupId = scopeGroup.getLiveGroupId();

			Group calendarGroup = _groupLocalService.getGroup(
				calendar.getGroupId());

			long calendarGroupId = calendarGroup.getGroupId();

			if (scopeGroup.isStagingGroup()) {
				if (calendarGroup.isStagingGroup()) {
					if (scopeGroupId != calendarGroupId) {
						calendar =
							_calendarLocalService.fetchCalendarByUuidAndGroupId(
								calendar.getUuid(),
								calendarGroup.getLiveGroupId());
					}
				}
				else if (scopeLiveGroupId == calendarGroupId) {
					Group stagingGroup = calendarGroup.getStagingGroup();

					calendar =
						_calendarLocalService.fetchCalendarByUuidAndGroupId(
							calendar.getUuid(), stagingGroup.getGroupId());
				}
			}
			else if (calendarGroup.isStagingGroup()) {
				calendar = _calendarLocalService.fetchCalendarByUuidAndGroupId(
					calendar.getUuid(), calendarGroup.getLiveGroupId());
			}

			if (calendar == null) {
				continue;
			}

			otherCalendars.add(calendar);
		}

		return otherCalendars;
	}

	private final CalendarLocalService _calendarLocalService;
	private final CalendarService _calendarService;
	private final GroupLocalService _groupLocalService;
	private final ThemeDisplay _themeDisplay;

}