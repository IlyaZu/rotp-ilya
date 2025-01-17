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

public class KillGuardianIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empMe;
    private final int empYou;
    private String monsterKey;
    
    public static KillGuardianIncident create(int e1, int e2, String key) {
        KillGuardianIncident inc = new KillGuardianIncident(e1, e2, key);
        return inc;
    }
    private KillGuardianIncident(int e1, int e2, String key) {
        super(100);
        empMe = e1;
        empYou = e2;
        monsterKey = key;
    }
    @Override
    public String title()         { return text("INC_KILLED_GUARDIAN_TITLE", text(monsterKey)); }
    @Override
    public String description()   { return decode(text("INC_KILLED_GUARDIAN_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[monster]", text(monsterKey));
        return s1;
    }
}
