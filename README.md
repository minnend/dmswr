# Safely Boosting Retirement Income by Harmonizing Drawdown Paths

Authors: Dave Marwood & David Minnen

This repo contains code used to explore the DMSWR and to generate figures for the paper. It also contains generated data files to help other researchers and financial plans apply and extend the DMSWR.

## Executive Summary

* Building on Bengen's famous 4% Rule (Bengen1994), this paper introduces the DMSWR, a technique for calculating withdrawal rates and retirement income levels that are safe, steady, and adjusted for inflation.

* The DMSWR, named after the authors' initials, addresses two commonly cited shortcomings of the 4% Rule. First, it addresses the *Starting Point Paradox* (Kitces2008) by ensuring that initial retirement incomes are stable regardless of the precise retirement date and short-term fluctuations in the stock market. Second, the DMSWR significantly reduces the risk of unnecessarily low retirement income that leads to large, unspent portfolios at the end retirement.

* The DMSWR provides withdrawal rates that are often substantially higher, and never lower, than the 4% Rule, which allows financial planners to recommend higher retirement incomes that are still safe.

* The core insight underpinning the DMSWR is that a retiree can safely follow the "drawdown path" of an earlier retiree who follows the 4% Rule and planned for a longer retirement. Since the earlier retiree's withdrawals are safe, the withdrawals of the new retiree must also be safe.

* The DMSWR is safe in the same way the 4% Rule is safe: the withdrawal rates have never failed in the past and, were DMSWR to fail in the future, the 4% Rule would also fail. Unlike many similar efforts to boost safe withdrawal rates, the DMSWR does not include additional parameters or heuristics that might fail at a time when following the 4% Rule would succeed.

* Historically, the mean DMSWR for 30-year retirements was 5.48%, the highest DMSWR was 13.3%, and the DMSWR exceeded the baseline withdrawal rate by more than 100 basis points over 55% of the time.

* "Re-retirement" using the DMSWR often allows incomes to increase during retirement. These higher incomes succeed whenever the 4% Rule succeeds.
