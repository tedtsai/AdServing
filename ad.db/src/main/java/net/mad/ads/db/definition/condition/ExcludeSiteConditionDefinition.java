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
package net.mad.ads.db.definition.condition;

import java.util.ArrayList;
import java.util.List;

import net.mad.ads.db.definition.ConditionDefinition;

/**
 * Steuerung auf welchen Seiten das Banner NICHT angezeigt werden soll
 * 
 * @author tmarx
 *
 */
public class ExcludeSiteConditionDefinition implements ConditionDefinition {
	
	private List<String> sites = new ArrayList<String>();
	
	public ExcludeSiteConditionDefinition () {
		
	}
	
	/**
	 * ID der Seiten auf denen das Banner NICHT angezeigt werden soll
	 */
	public List<String> getSites() {
		return this.sites;
	}
	public void addSite (String site) {
		this.sites.add(site);
	}
}
