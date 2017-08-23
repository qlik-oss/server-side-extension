using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Basic_example
{
    class CultureGuessingDateParser
    {
        public static readonly DateTime QlikDateBeforeFirstDate = new DateTime(1899, 12, 30);

        public static DateTime DateFromStringGuessingCulture(string dateString, string cultureIndicator)
        {
            var allCultures = CultureInfo.GetCultures(CultureTypes.AllCultures).Select(cultureInfo => new
            {
                CultureInfo = cultureInfo,
                EnglishName = cultureInfo.EnglishName.ToLowerInvariant(),
                NativeName = cultureInfo.NativeName.ToLowerInvariant()
            }).ToArray();


            string cultureIndicatorSearchString = cultureIndicator.ToLowerInvariant();

            var alreadySelectedCultures = new Dictionary<string, CultureInfo>();

            CultureInfo bestFitCulture = null;
            if (!alreadySelectedCultures.TryGetValue(cultureIndicatorSearchString, out bestFitCulture))
            {
                bestFitCulture = allCultures[0].CultureInfo;
                var bestScore =
                    Math.Min((int)ComputeLevenshteinDistance(cultureIndicatorSearchString, allCultures[0].EnglishName),
                        (int)ComputeLevenshteinDistance(cultureIndicatorSearchString, allCultures[0].NativeName));

                for (int cultureIndex = 1; cultureIndex < allCultures.Length; ++cultureIndex)
                {
                    var candidate = allCultures[cultureIndex];
                    var score =
                        Math.Min((int)ComputeLevenshteinDistance(cultureIndicatorSearchString, candidate.EnglishName),
                            (int)ComputeLevenshteinDistance(cultureIndicatorSearchString, candidate.NativeName));

                    if (score < bestScore)
                    {
                        bestFitCulture = candidate.CultureInfo;
                        bestScore = score;
                    }
                    else if (score == bestScore && candidate.CultureInfo.IsNeutralCulture && !bestFitCulture.IsNeutralCulture)
                    {
                        bestFitCulture = candidate.CultureInfo;
                        bestScore = score;
                    }
                }

                alreadySelectedCultures.Add(cultureIndicatorSearchString, bestFitCulture);
            }

            DateTime result;

            if (DateTime.TryParse(dateString, bestFitCulture, DateTimeStyles.None, out result))
            {
                return result;
            }
            else
            {
                return QlikDateBeforeFirstDate;
            }

        }


        private static int ComputeLevenshteinDistance(string source, string target)
        {
            int sourceLength = source.Length;
            int targetLength = target.Length;

            if (targetLength == 0)
            {
                return sourceLength;
            }

            if (sourceLength == 0)
            {
                return targetLength;
            }

            int[,] costMatrix = new int[sourceLength + 1, targetLength + 1];

            // Initial deletions
            for (int i = 0; i <= sourceLength; ++i)
            {
                costMatrix[i, 0] = i;
            }

            // Initial insertions
            for (int j = 0; j <= targetLength; ++j)
            {
                costMatrix[0, j] = j;
            }

            // All combinations
            for (int i = 1; i <= sourceLength; ++i)
            {
                for (int j = 1; j <= targetLength; ++j)
                {
                    int substitutionCost = (target[j - 1] == source[i - 1]) ? 0 : 1;

                    // Step 6
                    costMatrix[i, j] = Math.Min(
                        Math.Min(costMatrix[i - 1, j] + 1,  // Insertion
                            costMatrix[i, j - 1] + 1),          // Deletion
                        costMatrix[i - 1, j - 1] + substitutionCost);   // Substitution
                }
            }
            // Step 7
            return costMatrix[sourceLength, targetLength];
        }
    }
}
