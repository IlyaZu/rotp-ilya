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

import java.io.Serializable;
import java.util.Comparator;
import rotp.ui.notifications.TurnNotification;
import rotp.util.Base;

public abstract class DiplomaticIncident implements Base, Serializable {
    private static final long serialVersionUID = 1L;

    public float severity;
    public int duration;
    public int turnOccurred;

    public int timerKey()                { return -1; } // default -1 for timerKey index means no timer triggered
    public abstract String key();
    public abstract String title();
    public String description()          { return ""; }
    public boolean triggeredByAction()   { return true; }
    public String praiseMessageId()      { return ""; }
    public String warningMessageId()     { return ""; }
    public String declareWarId()         { return ""; }
    public int duration()                { return duration; }
    public int turnOccurred()            { return turnOccurred; }
    public void notifyOfPraise()         { }  // provides hook to avoid constant praise

    @Override
    public String toString() {  return concat(str(turnOccurred), ": ", title(), " = ", fmt(currentSeverity(),1)); }
    public boolean moreSevere(DiplomaticIncident inc) {
        if  (inc == null)
            return true;
        return Math.abs(currentSeverity()) > Math.abs(inc.currentSeverity());
    }

    public String decode(String s)       { return s.replace("[year]", str(turnOccurred)); }
    public String displayOrder()         { return TurnNotification.DIPLOMATIC_MESSAGE; }
    public float currentSeverity()	     { return severity*remainingTime()/duration(); }

    public boolean isForgotten()         { return remainingTime() <= 0; }
    public boolean isSpying()            { return false; }
    public boolean isAttacking()         { return false; }

    public boolean triggersPraise()      { return !praiseMessageId().isEmpty(); }
    public boolean triggersWarning()     { return !warningMessageId().isEmpty(); }
    public boolean triggersImmediateWar(){ return false; }
    public boolean triggersWar()         { return !declareWarId().isEmpty(); }

    private float remainingTime() {
            return Math.max(0, (turnOccurred() + duration() - galaxy().currentTurn()));
    }
    public static Comparator<DiplomaticIncident> DATE = (DiplomaticIncident o1, DiplomaticIncident o2) -> Integer.compare(o2.turnOccurred(), o1.turnOccurred());
}
