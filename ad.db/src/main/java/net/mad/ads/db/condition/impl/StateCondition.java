/**
 * Mad-Advertisement
 * Copyright (C) 2011 Thorsten Marx <thmarx@gmx.net>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.mad.ads.db.condition.impl;

import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import net.mad.ads.db.AdDBConstants;
import net.mad.ads.db.condition.Condition;
import net.mad.ads.db.db.request.AdRequest;
import net.mad.ads.db.definition.BannerDefinition;
import net.mad.ads.db.definition.condition.StateConditionDefinition;
import net.mad.ads.db.enums.ConditionDefinitions;
import net.mad.ads.db.enums.State;

public class StateCondition implements Condition {

	@Override
	public void addQuery(AdRequest request, BooleanQuery mainQuery) {
		if (request.getState() == null) {
			return;
		}
		int state = request.getState().getState();
		if (state == State.All.getState() || state == State.UNKNOWN.getState()) {
			return;
		}
		BooleanQuery query = new BooleanQuery();
		
		BooleanQuery temp = new BooleanQuery();
		temp.add(new TermQuery(new Term(AdDBConstants.ADDB_BANNER_STATE, String.valueOf(state))), Occur.SHOULD);
		temp.add(new TermQuery(new Term(AdDBConstants.ADDB_BANNER_STATE, String.valueOf(State.All.getState()))), Occur.SHOULD);
		
		query.add(temp, Occur.MUST);
		
		mainQuery.add(query, Occur.MUST);
	}

	@Override
	public void addFields(Document bannerDoc, BannerDefinition bannerDefinition) {
		
		StateConditionDefinition stDef = null;
		if (bannerDefinition.hasConditionDefinition(ConditionDefinitions.STATE)) {
			stDef = (StateConditionDefinition) bannerDefinition.getConditionDefinition(ConditionDefinitions.STATE);
		}
		
		if (stDef != null && stDef.getStates().size() > 0) {
			List<State> list = stDef.getStates();
			for (State state : list) {
				bannerDoc.add(new Field(AdDBConstants.ADDB_BANNER_STATE, String.valueOf(state.getState()), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		} else {
			bannerDoc.add(new Field(AdDBConstants.ADDB_BANNER_STATE, AdDBConstants.ADDB_BANNER_STATE_ALL, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
	}

}
