package com.github.tyrantsim.jtuo.optimizer;

import org.apache.commons.math3.distribution.BinomialDistribution;

import com.github.tyrantsim.jtuo.control.EvaluatedResults;
import com.github.tyrantsim.jtuo.sim.OptimizationMode;
import com.github.tyrantsim.jtuo.sim.Results;

public class TyrantOptimize {

    public static boolean modeOpenTheDeck = false;
    public static OptimizationMode optimizationMode = OptimizationMode.NOT_SET;
    public static double confidenceLevel = 0.99;
    public static boolean useHarmonicMean = false;
    int min_possible_score[] = {0, 0, 0, 10, 5, 5, 5, 0
//        #ifndef NQUEST
        , 0
//        #endif
        };
        int  max_possible_score[] = {100, 100, 100, 100, 65, 65, 100, 100
        //#ifndef NQUEST
        , 100
       // #endif
        };
    
    
    public Results computeScore(EvaluatedResults results, double[] factors)
    {
        Results last = new Results(0l, 0l, 0l, 0l); //, 0, 0, 0, 0, 0, results.second};
        double max_possible = max_possible_score[optimizationMode.ordinal()];
        for (int index = 0; index < results.getResults().size(); ++index) {
            last.wins += results.getResults().get(index).wins * factors[index];
            last.draws += results.getResults().get(index).draws * factors[index];
            last.losses += results.getResults().get(index).losses * factors[index];
            //results.second, results.first[index].points / max_possible, 1 - confidence_level
            BinomialDistribution bd = new BinomialDistribution(results.getTotalBattles(), 1 - confidenceLevel);
            // results.getResults().get(index).points / max_possible
            double lower_bound = bd.getSupportLowerBound() * max_possible; // find_lower_bound_on_p() * max_possible;
            double upper_bound = bd.getSupportUpperBound() * max_possible; //new BinomialDistribution(results.getTotalBattles(), results.getResults().get(index).points / max_possible, 1 - confidenceLevel).getSupportUpperBound() * max_possible;
            if (useHarmonicMean)
            {
                last.points += factors[index] / results.getResults().get(index).points;
                last.points_lower_bound += factors[index] / lower_bound;
                last.points_upper_bound += factors[index] / upper_bound;
            }
            else
            {
                last.points += results.getResults().get(index).points * factors[index];
                last.points_lower_bound += lower_bound * factors[index];
                last.points_upper_bound += upper_bound * factors[index];
            }
        }
        double factorSum = 0;
        for (int i = 0; i < factors.length; i++) {
            factorSum += factors[i]; 
        }

        last.wins /= factorSum * ( double)results.getTotalBattles();
        last.draws /= factorSum * ( double)results.getTotalBattles();
        last.losses /= factorSum * ( double)results.getTotalBattles();
        if (useHarmonicMean)
        {
            last.points = (long)factorSum / (results.getTotalBattles() * last.points);
            last.points_lower_bound = factorSum / last.points_lower_bound;
            last.points_upper_bound = factorSum / last.points_upper_bound;
        }
        else
        {
            last.points /= factorSum * results.getTotalBattles();
            last.points_lower_bound /= factorSum;
            last.points_upper_bound /= factorSum;
        }
        return last;
    }
    
}
