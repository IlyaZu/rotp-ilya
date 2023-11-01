/*
 * Copyright 2015-2020 Ray Fowler
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

public class SignDeclareWarIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    final int empOther;
    public static SignDeclareWarIncident create(Empire e1, Empire e2, Empire t) {
        SignDeclareWarIncident inc = new SignDeclareWarIncident(e1, e2, t);
        return inc;
    }
    private SignDeclareWarIncident(Empire e1, Empire e2, Empire t) {
        empMe = e1.id;
        empYou = e2.id;
        empOther = t.id;
        severity = 0;

        turnOccurred = galaxy().currentTurn();
        duration = 5;
    }
    @Override
    public String title()               { return text("INC_JOINT_WAR_TITLE"); }
    @Override
    public String key()                 { return concat("JointWar:", str(empOther)); }
    @Override
    public String decode(String s) {
        String s1 = s.replace("[year]", str(turnOccurred));
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = galaxy().empire(empOther).replaceTokens(s1, "other");
        return s1;
    }
}
