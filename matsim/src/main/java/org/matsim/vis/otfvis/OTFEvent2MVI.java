/* *********************************************************************** *
 * project: org.matsim.*
 * OTFEvent2MVI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vis.otfvis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.run.Events2Snapshot;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFFileWriterQSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.VisNetwork;

/**
 * 
 * This is called by org.matsim.run.otfvis.
 *
 * @author dstrippgen
 *
 */
public class OTFEvent2MVI extends OTFFileWriter {
	
	private final String eventFileName;

	private final OTFAgentsListHandler.Writer writer;

	private VisNetwork network;

	private Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

	public OTFEvent2MVI(VisNetwork net, String eventFileName, String outFileName, double interval_s) {
		super(interval_s, new OTFQSimServerQuadBuilder(net), outFileName, new OTFFileWriterQSimConnectionManagerFactory());
		this.network = net;
		this.eventFileName = eventFileName;
		this.writer = new OTFAgentsListHandler.Writer();
		this.writer.setSrc(this.positions );
	}

	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
	}

	private double lastTime=-1;

	public void convert(final Config config) {
		open();
		config.getQSimConfigGroup().setSnapshotFormat("none");
		config.getQSimConfigGroup().setSnapshotPeriod(this.interval_s);
		Events2Snapshot app = new Events2Snapshot();
		app.addExternalSnapshotWriter(this);
		app.run(new File(this.eventFileName), config, this.network.getNetwork());
		close();
	}

	@Override
	public void addAgent(AgentSnapshotInfo position) {
		//drop all parking vehicles
		if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
		this.positions.add( position );
	}

	@Override
	public void beginSnapshot(double time) {
		this.positions.clear();
		this.lastTime = time;
	}

	@Override
	public void endSnapshot() {
		try {
			dump((int)this.lastTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
