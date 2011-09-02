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
import net.mad.ads.db.definition.condition.DayConditionDefinition;
import net.mad.ads.db.enums.ConditionDefinitions;
import net.mad.ads.db.enums.Day;

/**
 * Bedingung die angiebt, an welchen Tage der Woche ein Banner angezeigt werden soll
 * 
 * 0 = an allen Tagen
 * 1 = Montag
 * 2 = Dienstag
 * 3 = Mittwoch
 * 4 = Donnerstag
 * 5 = Freitag
 * 6 = Samstag
 * 7 = Sonntag
 * 
 * @author thmarx
 *
 */
public class DayCondition implements Condition {

	@Override
	public void addQuery(AdRequest request, BooleanQuery mainQuery) {
		if (request.getDay() == null) {
			return;
		}
		int day = request.getDay().getDay();
		if (day == Day.All.getDay()) {
			return;
		}
		BooleanQuery query = new BooleanQuery();
		
		BooleanQuery temp = new BooleanQuery();
		temp.add(new TermQuery(new Term(AdDBConstants.ADDB_BANNER_DAY, String.valueOf(day))), Occur.SHOULD);
		temp.add(new TermQuery(new Term(AdDBConstants.ADDB_BANNER_DAY, String.valueOf(Day.All.getDay()))), Occur.SHOULD);
		
		query.add(temp, Occur.MUST);

		mainQuery.add(query, Occur.MUST);
	}

	@Override
	public void addFields(Document bannerDoc, BannerDefinition bannerDefinition) {
		DayConditionDefinition ddef = null;
		
		if (bannerDefinition.hasConditionDefinition(ConditionDefinitions.DAY)) {
			ddef = (DayConditionDefinition) bannerDefinition.getConditionDefinition(ConditionDefinitions.DAY);
		}
		
		
		if (ddef != null && ddef.getDays().size() > 0) {
			List<Day> list = ddef.getDays();
			for (Day day : list) {
				bannerDoc.add(new Field(AdDBConstants.ADDB_BANNER_DAY, String.valueOf(day.getDay()), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		} else {
			bannerDoc.add(new Field(AdDBConstants.ADDB_BANNER_DAY, AdDBConstants.ADDB_BANNER_DAY_ALL, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
	}

}