/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2024 Ilya Zushinskiy
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.incidents;

import rotp.model.empires.Empire;
import rotp.ui.diplomacy.DialogueManager;

public class GenocideIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empAttacker;
    private final int empVictim;

    public static void create(Empire victim, Empire attacker) {
        for (Empire emp: Empire.allContacts(victim, attacker)) {
            GenocideIncident inc = new GenocideIncident(emp, attacker, victim);
            emp.diplomatAI().noticeIncident(inc, attacker);
        }
    }
    private GenocideIncident(Empire obs, Empire att, Empire vic) {
        super(calculateSeverity(obs, att, vic));
        empAttacker = att.id;
        empVictim = vic.id;
    }
    private static float calculateSeverity(Empire obs, Empire att, Empire vic) {
        float severity = -50;
        if (att.alliedWith(obs.id) && obs.atWarWith(vic.id))
            severity /= 2;
        return severity;
    }
    @Override
    public String title()            { return text("INC_GENOCIDE_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_GENOCIDE_DESC")); }
    @Override
    public String warningMessageId() {  return DialogueManager.WARNING_GENOCIDE; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        return s1;
    }
}
