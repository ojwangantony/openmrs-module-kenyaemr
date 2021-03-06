/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.form.element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.kenyaemr.form.widget.ObjectSearchWidget;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Overrides regular <encounterLocation> tag
 */
public class EncounterLocationSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	protected static final Log log = LogFactory.getLog(EncounterLocationSubmissionElement.class);

	private String id;

	private ObjectSearchWidget widget;

	private ErrorWidget errorWidget;

	/**
	 * Constructs new location submission element
	 * @param context the form entry context
	 * @param parameters the tag parameters
	 */
	public EncounterLocationSubmissionElement(FormEntryContext context, Map<String, String> parameters) {

		widget = new ObjectSearchWidget(Location.class);
		errorWidget = new ErrorWidget();

		// Parse default value
		Location defaultLocation = null;
		if (context.getExistingEncounter() != null) {
			defaultLocation = context.getExistingEncounter().getLocation();
		} else {
			String defaultLocId = parameters.get("default");
			if (StringUtils.hasText(defaultLocId)) {
				defaultLocation = HtmlFormEntryUtil.getLocation(defaultLocId, context);
			}
		}

		defaultLocation = defaultLocation == null ? context.getDefaultLocation() : defaultLocation;
		widget.setInitialValue(defaultLocation);

		context.registerWidget(widget);
		context.registerErrorWidget(widget, errorWidget);

		// Set the id, if it has been specified
		if (parameters.get("id") != null) {
			id = parameters.get("id");
		}
	}

	/**
	 * @see HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();

		if (id != null) {
			ret.append("<span id=\"" + id + "\">");
			context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(widget), null, null, null);
			context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(errorWidget), null, null, null);
		}

		ret.append(widget.generateHtml(context));

		if (context.getMode() != FormEntryContext.Mode.VIEW) {
			ret.append(errorWidget.generateHtml(context));
		}

		if (id != null) {
			ret.append("</span>");
		}

		return ret.toString();
	}

	/**
	 * @see FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest request) {
		List<FormSubmissionError> errors = new ArrayList<FormSubmissionError>();
		try {
			Object value = widget.getValue(context, request);
			Location location = (Location) HtmlFormEntryUtil.convertToType(value.toString().trim(), Location.class);
			if (location == null) {
				throw new Exception("required");
			}
		} catch (Exception ex) {
			errors.add(new FormSubmissionError(context.getFieldName(errorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		return errors;
	}

	/**
	 * @see FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Object value = widget.getValue(session.getContext(), request);
		Location location = (Location) HtmlFormEntryUtil.convertToType(value.toString().trim(), Location.class);
		session.getSubmissionActions().getCurrentEncounter().setLocation(location);
	}
}