/**
* zaptain-mausi | Maui-Wrapper for short-text based STW subject indexing
* Copyright (C) 2016-2018  Martin Toepfer | ZBW -- Leibniz Information Centre for Economics
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package eu.zbw.a1.mausi;

public class StwAnnotation {

  public final int begin;

  public final int end;

  public final String cid;

  public final String matchingText;

  /**
   * @param cid
   *          concept id
   * @param begin
   *          offset
   * @param end
   *          offset + length
   */
  public StwAnnotation(String cid, String matchingText, final int begin, final int end) {
    this.cid = cid;
    this.matchingText = matchingText;
    this.begin = begin;
    this.end = end;
  }

  @Override
  public String toString() {
    return String.format("[%s, \"%s\", %d, %d]", cid, matchingText, begin, end);
  }
}
