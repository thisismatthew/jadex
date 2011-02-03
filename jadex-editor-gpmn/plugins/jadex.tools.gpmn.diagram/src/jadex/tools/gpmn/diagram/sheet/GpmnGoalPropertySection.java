/*
 * Copyright (c) 2009, Universität Hamburg
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package jadex.tools.gpmn.diagram.sheet;

import jadex.editor.model.common.properties.AbstractCommonPropertySection;
import jadex.editor.model.common.properties.ModifyEObjectCommand;
import jadex.tools.gpmn.ActivationPlan;
import jadex.tools.gpmn.Goal;
import jadex.tools.gpmn.GoalType;
import jadex.tools.gpmn.ModeType;
import jadex.tools.gpmn.PlanEdge;
import jadex.tools.gpmn.diagram.tools.SGpmnUtilities;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * @generated
 */
public class GpmnGoalPropertySection extends AbstractCommonPropertySection
{
	public static final String GOAL_CONFIGURATION_TITLE = "Goal Configuration";
	public static final String GOAL_TYPE_DESC 			= "Type:";
	public static final String GOAL_NAME_DESC 			= "Name:";
	public static final String GOAL_DESCRIPTION_DESC	= "Description:";
	public static final String CONTEXT_CONDITION_DESC	= "Context Condition:";
	public static final String MAINTAIN_CONDITION_DESC	= "Maintain Condition:";
	public static final String TARGET_CONDITION_DESC	= "Target Condition:";
	public static final String PLAN_SEMANTICS_DESC		= "Activation Plan Semantics:";
	
	protected static final Map<String, GoalType> GOAL_TYPE_MAP = new HashMap<String, GoalType>();
	static
	{
		GOAL_TYPE_MAP.put("Achieve Goal", GoalType.ACHIEVE_GOAL);
		GOAL_TYPE_MAP.put("Maintain Goal", GoalType.MAINTAIN_GOAL);
		GOAL_TYPE_MAP.put("Perform Goal", GoalType.PERFORM_GOAL);
		//GOAL_TYPE_MAP.put("Query Goal");
	}
	
	protected static final Map<String, ModeType> PLAN_SEMANTICS_MAP = new HashMap<String, ModeType>();
	static
	{
		PLAN_SEMANTICS_MAP.put("Parallel", ModeType.PARALLEL);
		PLAN_SEMANTICS_MAP.put("Sequential", ModeType.SEQUENTIAL);
		PLAN_SEMANTICS_MAP.put("Mixed/None", null);
	}
	
	protected Map<GoalType, Integer> invGoalTypeMap;
	protected Map<ModeType, Integer> invPlanSemanticsMap;
	
	protected Map<String, Label> labels;
	protected Map<String, Control> controls;
	
	protected Group confGroup;
	
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage)
	{
		//super.createControls(parent, aTabbedPropertySheetPage);
		
		labels = new HashMap<String, Label>();
		controls = new HashMap<String, Control>();
		parent.setLayout(new FillLayout());
		
		confGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
		addDisposable(confGroup);
		confGroup.setLayout(new FillLayout());
		((FillLayout) confGroup.getLayout()).type = SWT.HORIZONTAL;
		confGroup.setText(GOAL_CONFIGURATION_TITLE);
		
		Composite leftComposite = new Composite(confGroup, SWT.NULL);
		addDisposable(leftComposite);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		leftComposite.setLayout(gridLayout);
		
		addGoalTypeControl(leftComposite);
		addContextConditionControl(leftComposite);
		addMaintainConditionControl(leftComposite);
		addTargetConditionControl(leftComposite);
		
		Composite rightComposite = new Composite(confGroup, SWT.NULL);
		addDisposable(rightComposite);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		rightComposite.setLayout(gridLayout);
		
		addPlanSemanticsControl(rightComposite);
		addNameControl(rightComposite);
		addDescriptionControl(rightComposite);
	}
	
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection)
	{
		super.setInput(part, selection);
		refreshControls();
	}
	
	@Override
	public boolean shouldUseExtraSpace()
	{
		return true;
	}
	
	protected void refreshControls()
	{
		if (!(modelElement instanceof Goal))
			return;
		
		Goal goal = (Goal) modelElement;
		
		((Combo) controls.get(GOAL_TYPE_DESC)).select(invGoalTypeMap.get(goal.getGoalType()));
		setTextControlValue(((Text) controls.get(GOAL_NAME_DESC)), goal.getName());
		setTextControlValue(((Text) controls.get(CONTEXT_CONDITION_DESC)), goal.getContextcondition());
		setTextControlValue(((Text) controls.get(MAINTAIN_CONDITION_DESC)), goal.getMaintaincondition());
		setTextControlValue(((Text) controls.get(TARGET_CONDITION_DESC)), goal.getTargetcondition());
		
		boolean mVisible = GoalType.MAINTAIN_GOAL.equals(goal.getGoalType());
		Label label = labels.get(MAINTAIN_CONDITION_DESC);
		Control control = controls.get(MAINTAIN_CONDITION_DESC);
		label.setVisible(mVisible);
		control.setVisible(mVisible);
		((GridData) label.getLayoutData()).exclude = !mVisible;
		((GridData) control.getLayoutData()).exclude = !mVisible;
		
		mVisible = GoalType.ACHIEVE_GOAL.equals(goal.getGoalType());
		label = labels.get(TARGET_CONDITION_DESC);
		control = controls.get(TARGET_CONDITION_DESC);
		label.setVisible(mVisible);
		control.setVisible(mVisible);
		((GridData) label.getLayoutData()).exclude = !mVisible;
		((GridData) control.getLayoutData()).exclude = !mVisible;
		
		ModeType psMode = null;
		for (PlanEdge planEdge : SGpmnUtilities.getPlanEdges(goal))
		{
			if (planEdge.getTarget() instanceof ActivationPlan)
			{
				ActivationPlan ap = (ActivationPlan) planEdge.getTarget();
				if (!(ap.getMode().equals(psMode)) && psMode != null)
				{
					System.out.println(ap.getMode() + " " + psMode);
					psMode = null;
					break;
				}
				psMode = ap.getMode();
			}
		}
		((Combo) controls.get(PLAN_SEMANTICS_DESC)).select(invPlanSemanticsMap.get(psMode));
		((Combo) controls.get(PLAN_SEMANTICS_DESC)).setEnabled(psMode != null);
		
		confGroup.layout();
		confGroup.update();
		Control[] children = confGroup.getChildren();
		for (int i = 0; i < children.length; ++i)
		{
			((Composite) children[i]).layout();
			children[i].update();
		}
	}
	
	protected void setTextControlValue(Text control, String value)
	{
		if (value != null)
		{
			control.setText(value);
		}
	}
	
	protected void addGoalTypeControl(Composite parent)
	{
		Label goalTypeLabel = new Label(parent, SWT.LEFT);
		addDisposable(goalTypeLabel);
		labels.put(GOAL_TYPE_DESC, goalTypeLabel);
		goalTypeLabel.setText(GOAL_TYPE_DESC);
		GridData gd = new GridData();
		gd.widthHint = 200;
		goalTypeLabel.setLayoutData(gd);
		
		Combo goalTypeCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		addDisposable(goalTypeCombo);
		controls.put(GOAL_TYPE_DESC, goalTypeCombo);
		gd = new GridData();
		gd.widthHint = 300;
		goalTypeCombo.setLayoutData(gd);
		
		invGoalTypeMap = new HashMap<GoalType, Integer>(GOAL_TYPE_MAP.size());
		int index = 0;
		for (Map.Entry<String, GoalType> entry : GOAL_TYPE_MAP.entrySet())
		{
			goalTypeCombo.add(entry.getKey());
			invGoalTypeMap.put(entry.getValue(), index++);
		}
		
		goalTypeCombo.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent se)
			{
				final GoalType gt = GOAL_TYPE_MAP.get(((Combo) controls.get(GOAL_TYPE_DESC)).getText());
				if (gt != null && modelElement != null)
				{
					dispatchCommand(new ModifyEObjectCommand(modelElement,
					"Change GoalType Property")
					{
						protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
						{
							((Goal) modelElement).setGoalType(gt);
							return CommandResult.newOKCommandResult();
						}
					});
					refreshControls();
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent se)
			{
				widgetSelected(se);
			}
		});
	}
	
	protected void addNameControl(final Composite parent)
	{
		Text nameText = addLabeledTextControl(parent, GOAL_NAME_DESC);
		nameText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				final String name = ((Text) e.widget).getText();
				dispatchCommand(new ModifyEObjectCommand(modelElement,
				"Change Name Property")
				{
					protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
					{
						parent.layout();
						if (name.isEmpty())
							((Goal) modelElement).unsetName();
						else
							((Goal) modelElement).setName(name);
						return CommandResult.newOKCommandResult();
					}
				});
			}
		});
	}
	
	protected void addDescriptionControl(final Composite parent)
	{
		Text nameText = addLabeledTextControl(parent, GOAL_DESCRIPTION_DESC);
		nameText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				final String description = ((Text) e.widget).getText();
				dispatchCommand(new ModifyEObjectCommand(modelElement,
				"Change Description Property")
				{
					protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
					{
						parent.layout();
						if (description.isEmpty())
							((Goal) modelElement).unsetDescription();
						else
							((Goal) modelElement).setDescription(description);
						return CommandResult.newOKCommandResult();
					}
				});
			}
		});
	}
	
	protected void addContextConditionControl(final Composite parent)
	{
		Text ccText = addLabeledTextControl(parent, CONTEXT_CONDITION_DESC);
		ccText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				final String cc = ((Text) e.widget).getText();
				dispatchCommand(new ModifyEObjectCommand(modelElement,
				"Change Contextcondition Property")
				{
					protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
					{
						parent.layout();
						if (cc.isEmpty())
							((Goal) modelElement).unsetContextcondition();
						else
							((Goal) modelElement).setContextcondition(cc);
						return CommandResult.newOKCommandResult();
					}
				});
			}
		});
	}
	
	protected void addMaintainConditionControl(final Composite parent)
	{
		Text mCondControl = addLabeledTextControl(parent, MAINTAIN_CONDITION_DESC);
		mCondControl.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				final String mc = ((Text) e.widget).getText();
				dispatchCommand(new ModifyEObjectCommand(modelElement,
				"Change Maintain Condition Property")
				{
					protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
					{
						parent.layout();
						if (mc.isEmpty())
							((Goal) modelElement).unsetMaintaincondition();
						else
							((Goal) modelElement).setMaintaincondition(mc);
						return CommandResult.newOKCommandResult();
					}
				});
			}
		});
	}
	
	protected void addTargetConditionControl(final Composite parent)
	{
		Text tCondControl = addLabeledTextControl(parent, TARGET_CONDITION_DESC);
		tCondControl.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				final String tc = ((Text) e.widget).getText();
				dispatchCommand(new ModifyEObjectCommand(modelElement,
				"Change Achieve Condition Property")
				{
					protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
					{
						parent.layout();
						if (tc.isEmpty())
							((Goal) modelElement).unsetTargetcondition();
						else
							((Goal) modelElement).setTargetcondition(tc);
						return CommandResult.newOKCommandResult();
					}
				});
			}
		});
	}
	
	protected void addPlanSemanticsControl(final Composite parent)
	{
		Label label = new Label(parent, SWT.LEFT);
		addDisposable(label);
		labels.put(PLAN_SEMANTICS_DESC, label);
		label.setText(PLAN_SEMANTICS_DESC);
		GridData gd = new GridData();
		label.setLayoutData(gd);
		
		Combo planSemanticsCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		addDisposable(planSemanticsCombo);
		controls.put(PLAN_SEMANTICS_DESC, planSemanticsCombo);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		planSemanticsCombo.setLayoutData(gd);
		
		invPlanSemanticsMap = new HashMap<ModeType, Integer>(PLAN_SEMANTICS_MAP.size());
		int index = 0;
		for (Map.Entry<String, ModeType> entry : PLAN_SEMANTICS_MAP.entrySet())
		{
			planSemanticsCombo.add(entry.getKey());
			invPlanSemanticsMap.put(entry.getValue(), index++);
		}
		
		planSemanticsCombo.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent se)
			{
				final ModeType mt = PLAN_SEMANTICS_MAP.get(((Combo) controls.get(PLAN_SEMANTICS_DESC)).getText());
				if (modelElement != null)
				{
					if (mt != null)
					{
						dispatchCommand(new ModifyEObjectCommand(modelElement,
						"Change Plan Semantics")
						{
							protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
							{
								for (PlanEdge planEdge : SGpmnUtilities.getPlanEdges(((Goal) modelElement)))
									if (planEdge.getTarget() instanceof ActivationPlan)
										((ActivationPlan) planEdge.getTarget()).setMode(mt);
								
								return CommandResult.newOKCommandResult();
							}
						});
					}
					refreshControls();
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent se)
			{
				widgetSelected(se);
			}
		});
	}
	
	protected Text addLabeledTextControl(Composite parent, String id)
	{
		return addLabeledTextControl(parent, id, SWT.MULTI | SWT.BORDER);
	}
	
	protected Text addLabeledTextControl(Composite parent, String id, int style)
	{
		Label label = new Label(parent, SWT.LEFT);
		addDisposable(label);
		labels.put(id, label);
		label.setText(id);
		label.setLayoutData(new GridData());
		
		Text text = new Text(parent, style);
		addDisposable(text);
		controls.put(id, text);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		text.setLayoutData(gd);
		
		return text;
	}
	
	protected void dispatchCommand(ModifyEObjectCommand cmd)
	{
		try
		{
			cmd.execute(null, null);
		}
		catch (ExecutionException e)
		{
			e.printStackTrace();
		}
	}
}
