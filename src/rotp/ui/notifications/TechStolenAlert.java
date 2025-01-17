/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024 Ilya Zushinskiy
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
package rotp.ui.notifications;

import rotp.model.game.GameSession;

public class TechStolenAlert extends GameAlert {
    private final int empSpy;
    public static void create(int emp) {
        GameSession.instance().addAlert(new TechStolenAlert(emp));
    }
    @Override
    public String description() {
        String desc = text("MAIN_ALERT_TECH_STOLEN");
        desc = galaxy().empire(empSpy).replaceTokens(desc, "alien");
        return desc;
    }
    private TechStolenAlert(int emp) {
        empSpy = emp;
    }
}
